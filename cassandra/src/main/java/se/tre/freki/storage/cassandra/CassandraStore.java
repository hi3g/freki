package se.tre.freki.storage.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.transform;
import static se.tre.freki.storage.cassandra.CassandraLabelId.fromLong;
import static se.tre.freki.storage.cassandra.CassandraLabelId.toLong;

import se.tre.freki.labels.LabelException;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.meta.Annotation;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.query.DataPoint;
import se.tre.freki.storage.Store;
import se.tre.freki.storage.cassandra.functions.FirstOrAbsentFunction;
import se.tre.freki.storage.cassandra.functions.IsEmptyFunction;
import se.tre.freki.storage.cassandra.functions.ToVoidFunction;
import se.tre.freki.storage.cassandra.query.DataPointIterator;
import se.tre.freki.storage.cassandra.query.SpeculativePartitionIterator;
import se.tre.freki.storage.cassandra.statements.AddPointStatements;
import se.tre.freki.storage.cassandra.statements.AddPointStatements.AddPointStatementMarkers;
import se.tre.freki.storage.cassandra.statements.FetchPointsStatements;
import se.tre.freki.storage.cassandra.statements.FetchPointsStatements.SelectPointStatementMarkers;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An implementation of {@link Store} that uses Cassandra as the underlying storage backend.
 */
public class CassandraStore extends Store {
  private static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  /**
   * The Cassandra cluster that we are connected to.
   */
  private final Cluster cluster;
  /**
   * The current Cassandra session.
   */
  private final Session session;

  /**
   * A time provider to tell the current time.
   */
  private final Clock clock;

  /**
   * The statement used by the {@link #addPoint} method.
   */
  private final PreparedStatement addFloatStatement;
  private final PreparedStatement addDoubleStatement;
  private final PreparedStatement addLongStatement;

  /**
   * Statement to fetch the data points of a single time series between two timestamps.
   */
  private final PreparedStatement fetchTimeSeriesStatement;

  /**
   * The statement used by the {@link #createLabel} method.
   */
  private PreparedStatement createIdStatement;
  /**
   * Used for {@link #renameLabel}, the one that does rename.
   */
  private PreparedStatement updateNameUidStatement;
  /**
   * The statement used when trying to get name or id.
   */
  private PreparedStatement getNameStatement;
  private PreparedStatement getIdStatement;

  private final IndexStrategy addPointIndexingStrategy;

  /**
   * The statements used when trying to get {@link #getMeta(LabelId, LabelType)} or update meta
   * {@link #updateMeta(LabelMeta)}.
   */
  private PreparedStatement getMetaStatement;
  private PreparedStatement updateMetaStatement;

  /**
   * Create a new instance that will use the provided Cassandra cluster and session instances.
   *
   * @param cluster A built and configured cluster instance
   * @param session A configured and connected session instance
   * @param clock A Clock to generate times for the time series.
   */
  public CassandraStore(final Cluster cluster,
                        final Session session,
                        final Clock clock,
                        final IndexStrategy addPointIndexingStrategy) {
    this.cluster = checkNotNull(cluster);
    this.session = checkNotNull(session);
    this.clock = checkNotNull(clock);

    this.addPointIndexingStrategy = checkNotNull(addPointIndexingStrategy);

    final AddPointStatements addPointStatements = new AddPointStatements(session);
    this.addFloatStatement = addPointStatements.addFloatStatement();
    this.addDoubleStatement = addPointStatements.addDoubleStatement();
    this.addLongStatement = addPointStatements.addLongStatement();

    final FetchPointsStatements fetchPointsStatements = new FetchPointsStatements(session);
    this.fetchTimeSeriesStatement = fetchPointsStatements.selectDataPointsStatement();

    prepareStatements();
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                         final long timestamp,
                                         final float value) {
    final BoundStatement addPointStatement = addFloatStatement.bind()
        .setFloat(AddPointStatementMarkers.VALUE.ordinal(), value);
    return addPoint(addPointStatement, tsuid.metric(), tsuid.tags(), timestamp);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                         final long timestamp,
                                         final double value) {
    final BoundStatement addPointStatement = addDoubleStatement.bind()
        .setDouble(AddPointStatementMarkers.VALUE.ordinal(), value);
    return addPoint(addPointStatement, tsuid.metric(), tsuid.tags(), timestamp);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                         final long timestamp,
                                         final long value) {
    final BoundStatement addPointStatement = addLongStatement.bind()
        .setLong(AddPointStatementMarkers.VALUE.ordinal(), value);
    return addPoint(addPointStatement, tsuid.metric(), tsuid.tags(), timestamp);
  }

  @Nonnull
  private ListenableFuture<Void> addPoint(final BoundStatement addPointStatement,
                                          final LabelId metric,
                                          final List<LabelId> tags,
                                          final long timestamp) {
    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric, tags);
    final long baseTime = BaseTimes.baseTimeFor(timestamp);

    addPointStatement.setBytesUnsafe(AddPointStatementMarkers.ID.ordinal(), timeSeriesId);
    addPointStatement.setLong(AddPointStatementMarkers.BASE_TIME.ordinal(), baseTime);
    addPointStatement.setLong(AddPointStatementMarkers.TIMESTAMP.ordinal(), timestamp);
    addPointStatement.setLong(AddPointStatementMarkers.USING_TIMESTAMP.ordinal(), timestamp);

    final ResultSetFuture future = session.executeAsync(addPointStatement);

    addPointIndexingStrategy.indexTimeseriesId(metric, tags, timeSeriesId);

    return transform(future, new ToVoidFunction());
  }

  /**
   * Check if either of (id, type) and (name, type) are taken or if both are available. If either of
   * the combinations already are taken the returned future will contain an {@link LabelException}.
   *
   * @param id The id to check if it is available
   * @param name The name to check if it is available
   * @param type The type of id and name to check if it available
   * @return A future that contains an exception if either of the above combinations were taken.
   * Otherwise a future with meaningless contents will be returned.
   */
  private ListenableFuture<Void> checkAvailable(final long id,
                                                final String name,
                                                final LabelType type) {
    ImmutableList<ListenableFuture<Boolean>> availableList =
        ImmutableList.of(isIdAvailable(id, type), isNameAvailable(name, type));
    final ListenableFuture<List<Boolean>> availableFuture = Futures.allAsList(availableList);

    return transform(availableFuture, new AsyncFunction<List<Boolean>, Void>() {
      @Override
      public ListenableFuture<Void> apply(final List<Boolean> available) {
        // These are in the same order as they are provided in the call
        // to Futures#allAsList.
        final Boolean idAvailable = available.get(0);
        final Boolean nameAvailable = available.get(1);

        if (!idAvailable) {
          return Futures.immediateFailedFuture(
              new LabelException(fromLong(id), type, "Id was already taken"));
        }

        if (!nameAvailable) {
          return Futures.immediateFailedFuture(
              new LabelException(name, type, "Name was already taken"));
        }

        return Futures.immediateFuture(null);
      }
    });
  }

  @Override
  public void close() {
    cluster.close();
  }

  /**
   * Save a new identifier with the provided information in Cassandra. This will not perform any
   * checks to see if the id already exists, you are expected to have done so already.
   *
   * @param id The id to associate with the provided name
   * @param name The name to save
   * @param type The type of id to save
   * @return A future containing the newly saved identifier
   */
  protected ListenableFuture<LabelId> createId(final long id,
                                               final String name,
                                               final LabelType type) {
    final Date createTimestamp = Date.from(clock.instant());
    final ResultSetFuture save = session.executeAsync(
        createIdStatement.bind(id, type.toValue(), createTimestamp, name,
            name, type.toValue(), createTimestamp, id));

    return transform(save, new AsyncFunction<ResultSet, LabelId>() {
      @Override
      public ListenableFuture<LabelId> apply(final ResultSet result) {
        // The Cassandra driver will have thrown an exception if the insertion
        // failed in which case we would not be here so just return the id we
        // sent to Cassandra.
        return Futures.<LabelId>immediateFuture(fromLong(id));
      }
    });
  }

  /**
   * Allocate an ID for the provided (name, type). This will attempt to generate an ID that is
   * likely to be available. It will then check if this information is available and finally save
   * the information if it is. If the information could be saved the ID will be returned in a
   * future, otherwise the future will contain an {@link LabelException}.
   *
   * @param name The name to create an ID for
   * @param type The type of name to create an ID for
   * @return A future that contains the newly create ID if successful, otherwise the future will
   * contain a {@link LabelException}.
   */
  @Nonnull
  @Override
  public ListenableFuture<LabelId> createLabel(final String name,
                                               final LabelType type) {
    // This discards half the hash but it should still work ok with murmur3.
    final long id = CassandraLabelId.generateId(name, type);

    // This does not protect us against someone trying to create the same
    // information in parallel but it is a convenience to the user so that we
    // do not even try to create if we can find an existing ID with the
    // information we are trying to create now.
    ListenableFuture<Void> availableFuture = checkAvailable(id, name, type);

    return transform(availableFuture, new AsyncFunction<Void, LabelId>() {
      @Override
      public ListenableFuture<LabelId> apply(final Void available) {
        // #checkAvailable will have thrown an exception if the id or name was
        // not available and if it did we would not be there. Thus we are now
        // free to create the id.
        return createId(id, name, type);
      }
    });
  }

  @Nonnull
  @Override
  public ListenableFuture<Boolean> renameLabel(final String newName,
                                               final LabelId id,
                                               final LabelType type) {
    // Get old name
    final ListenableFuture<List<Row>> oldNameFuture = getNameRows(id, type);

    return transform(oldNameFuture, new AsyncFunction<List<Row>, Boolean>() {
      @Override
      public ListenableFuture<Boolean> apply(final List<Row> rows) {
        final Row firstrow = rows.get(0);
        final String oldName = firstrow.getString("name");
        final Date creationTime = firstrow.getDate("creation_time");

        final ResultSetFuture nameIdFuture = session.executeAsync(
            updateNameUidStatement.bind(newName, toLong(id), type.toValue(), creationTime, oldName,
                type.toValue(), creationTime, newName, type.toValue(), creationTime, toLong(id)));

        return transform(nameIdFuture, new Function<ResultSet, Boolean>() {
          @Override
          public Boolean apply(final ResultSet update) {
            if (!update.wasApplied()) {
              LOG.warn("A rename of a label was requested but could not be completed.");
            }

            return update.wasApplied();
          }
        });
      }
    });
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteLabel(final String name,
                                            final LabelType type) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Nonnull
  @Override
  public ListenableFuture<Optional<LabelId>> getId(final String name,
                                                   final LabelType type) {
    ListenableFuture<List<LabelId>> idsFuture = getIds(name, type);
    return transform(idsFuture, new FirstOrAbsentFunction<LabelId>());
  }

  @Nonnull
  @Override
  public ListenableFuture<Optional<String>> getName(final LabelId id,
                                                    final LabelType type) {
    ListenableFuture<List<String>> namesFuture = getNames(id, type);
    return transform(namesFuture, new FirstOrAbsentFunction<String>());
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteAnnotation(final LabelId metric,
                                                 final ImmutableMap<LabelId, LabelId> tags,
                                                 final long startTime) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Nonnull
  @Override
  public ListenableFuture<Integer> deleteAnnotations(final LabelId metric,
                                                     final ImmutableMap<LabelId, LabelId> tags,
                                                     final long startTime,
                                                     final long endTime) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Nonnull
  @Override
  public ListenableFuture<Annotation> getAnnotation(final LabelId metric,
                                                    final ImmutableMap<LabelId, LabelId> tags,
                                                    final long startTime) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Nonnull
  @Override
  public ListenableFuture<Boolean> updateAnnotation(Annotation annotation) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Nonnull
  @Override
  public ListenableFuture<Optional<LabelMeta>> getMeta(final LabelId id,
                                                       final LabelType type) {

    final ListenableFuture<Optional<Row>> metas = getOptionalMeta(id, type);
    return transform(metas, new Function<Optional<Row>, Optional<LabelMeta>>() {
      @Nullable
      @Override
      public Optional<LabelMeta> apply(final Optional<Row> result) {
        if (!result.isPresent()) {
          return Optional.absent();
        }

        final Row row = result.get();

        return Optional.of(
            LabelMeta.create(CassandraLabelId.fromLong(row.getLong("label_id")),
                LabelType.fromValue(row.getString("type")), row.getString("name"),
                row.getString("meta_description"),
                row.getDate("creation_time").getTime()));
      }
    });

  }

  /**
   * A helper function to getMeta. It gets a Row containing meta information from the label with the
   * LabelId id and LabelType type.
   *
   * @param id The label id to fetch.
   * @param type the label type to fetch.
   * @return Returns absent if there was no meta for that id and type.
   */
  @Nonnull
  ListenableFuture<Optional<Row>> getOptionalMeta(final LabelId id,
                                                  final LabelType type) {

    final ListenableFuture<List<Row>> rowFuture = fetchLabelRows(id, type,
        getMetaStatement.bind(toLong(id), type.toValue()));

    return transform(rowFuture, new FirstOrAbsentFunction<Row>());
  }

  /**
   * Use this function to check for duplicates.
   *
   * @param id The LabelId we were looking for in the statement. For logging purpose.
   * @param type The type we were looking for in the statement. For logging purpose.
   * @param statement The statement we wish to execute.
   * @return Returns a list of rows.
   */
  @Nonnull
  ListenableFuture<List<Row>> fetchLabelRows(final LabelId id,
                                             final LabelType type,
                                             final BoundStatement statement) {

    final ResultSetFuture future = session.executeAsync(statement);

    return transform(future, new Function<ResultSet, List<Row>>() {
      @Override
      public List<Row> apply(final ResultSet result) {

        final List<Row> rows = result.all();

        if (rows.size() > 1) {
          LOG.error("Found duplicate IDs for ({}, {})", id, type);
        }

        return rows;
      }
    });
  }

  /**
   * Use this function to check for duplicates.
   *
   * @param name The name we were looking for in the statement. For logging purpose.
   * @param type The type we were looking for in the statement. For logging purpose.
   * @param statement The statement we wish to execute.
   * @return Returns a list of rows.
   */
  @Nonnull
  ListenableFuture<List<Row>> fetchLabelRows(final String name,
                                             final LabelType type,
                                             final BoundStatement statement) {

    final ResultSetFuture future = session.executeAsync(statement);

    return transform(future, new Function<ResultSet, List<Row>>() {
      @Override
      public List<Row> apply(final ResultSet result) {

        final List<Row> rows = result.all();

        if (rows.size() > 1) {
          LOG.error("Found duplicate IDs for ({}, {})", name, type);
        }

        return rows;
      }
    });
  }

  @Nonnull
  @Override
  public ListenableFuture<Boolean> updateMeta(LabelMeta meta) {
    final ResultSetFuture updateMetaFuture = session.executeAsync(
        updateMetaStatement.bind(meta.description(), toLong(meta.identifier()),
            meta.type().toValue(), new Date(meta.created())));

    return transform(updateMetaFuture, new Function<ResultSet, Boolean>() {
      @Nullable
      @Override
      public Boolean apply(final ResultSet input) {
        return input.wasApplied();
      }
    });
  }

  /**
   * Fetch all data points for the given time series that are within the given time range indicated
   * by {@code startTime} and {@code endTime}.
   *
   * <p>The returned iterator will try to prefetch as necessary to prevent blockage but may not
   * always succeed in doing so.
   *
   * @param timeSeriesId The time series to fetch the data points for
   * @param startTime The lower bound to timestamp to fetch data points within
   * @param endTime The upper bound to timestamp to fetch data points within
   * @return An iterator that will loop over all found data points.
   */
  protected Iterator<? extends DataPoint> fetchTimeSeries(
      final ByteBuffer timeSeriesId,
      final long startTime,
      final long endTime) {
    final Iterator<Row> rows = new SpeculativePartitionIterator<>(
        BaseTimes.baseTimesBetween(startTime, endTime),
        new Function<Long, ResultSetFuture>() {
          @Nullable
          @Override
          public ResultSetFuture apply(final Long baseTime) {
            return fetchTimeSeriesPartition(timeSeriesId, baseTime, startTime, endTime);
          }
        });

    return DataPointIterator.iteratorFor(rows);
  }

  /**
   * Fetch the data points in the partition indicated by {@code timeSeriesId} and {@code baseTime}
   * that are within the provided time bounds.
   *
   * @param timeSeriesId The time series to fetch the data points for
   * @param baseTime The base time as normalized by {@link BaseTimes#baseTimeFor(long)}
   * @param startTime The lower bound to timestamp to fetch data points within
   * @param endTime The upper bound to timestamp to fetch data points within
   * @return A future that on completion will contain a paged iterable of rows
   */
  protected ResultSetFuture fetchTimeSeriesPartition(final ByteBuffer timeSeriesId,
                                                     final long baseTime,
                                                     final long startTime,
                                                     final long endTime) {
    return session.executeAsync(fetchTimeSeriesStatement.bind()
        .setBytesUnsafe(SelectPointStatementMarkers.ID.ordinal(), timeSeriesId)
        .setLong(SelectPointStatementMarkers.BASE_TIME.ordinal(), baseTime)
        .setLong(SelectPointStatementMarkers.LOWER_TIMESTAMP.ordinal(), startTime)
        .setLong(SelectPointStatementMarkers.UPPER_TIMESTAMP.ordinal(), endTime));
  }

  /**
   * Fetch the first two IDs that are associated with the provided name and type.
   *
   * @param name The name to fetch IDs for
   * @param type The type of IDs to fetch
   * @return A future with a list of the first two found IDs
   */
  @Nonnull
  ListenableFuture<List<LabelId>> getIds(final String name,
                                         final LabelType type) {

    final ListenableFuture<List<Row>> idFuture = fetchLabelRows(name, type,
        getIdStatement.bind(name, type.toValue()));

    return transform(idFuture, new Function<List<Row>, List<LabelId>>() {
      @Nullable
      @Override
      public List<LabelId> apply(final List<Row> rows) {
        ImmutableList.Builder<LabelId> builder = ImmutableList.builder();

        for (final Row row : rows) {
          final long id = row.getLong("label_id");
          builder.add(fromLong(id));
        }

        return builder.build();
      }
    });
  }

  /**
   * Fetch the first two names that are associated with the provided id and type.
   *
   * @param id The id to fetch names for
   * @param type The type of names to fetch
   * @return A future with a list of the first two found names
   */
  @Nonnull
  ListenableFuture<List<String>> getNames(final LabelId id,
                                          final LabelType type) {

    final ListenableFuture<List<Row>> namesFuture = fetchLabelRows(id, type,
        getNameStatement.bind(toLong(id), type.toValue()));

    return transform(namesFuture, new Function<List<Row>, List<String>>() {
      @Override
      public List<String> apply(final List<Row> rows) {
        final ImmutableList.Builder<String> names = ImmutableList.builder();

        for (final Row row : rows) {
          final String name = row.getString("name");
          names.add(name);
        }

        return names.build();
      }
    });
  }

  @Nonnull
  ListenableFuture<List<Row>> getNameRows(final LabelId id,
                                          final LabelType type) {

    ResultSetFuture namesFuture = session.executeAsync(
        getNameStatement.bind(toLong(id), type.toValue()));

    return transform(namesFuture, new Function<ResultSet, List<Row>>() {
      @Override
      public List<Row> apply(final ResultSet result) {

        final ImmutableList<Row> rows = ImmutableList.copyOf(result.all());

        if (rows.size() > 1) {
          LOG.error("Found duplicate ID to name mapping for ID {} with type {}", id, type);
        }

        return rows;
      }
    });

  }

  public Session getSession() {
    return session;
  }

  /**
   * Check if (id, type) is available and return a future that contains a boolean that will be true
   * if the id is available or false if otherwise.
   *
   * @param id The name to check
   * @param type The type to check
   * @return A future that contains a boolean that indicates if the id was available
   */
  private ListenableFuture<Boolean> isIdAvailable(final long id,
                                                  final LabelType type) {
    return transform(getNames(fromLong(id), type), new IsEmptyFunction());
  }

  /**
   * Check if (name, type) is available and return a future that contains a boolean that will be
   * true if the name is available or false if otherwise.
   *
   * @param name The name to check
   * @param type The type to check
   * @return A future that contains a boolean that indicates if the name was available
   */
  private ListenableFuture<Boolean> isNameAvailable(final String name,
                                                    final LabelType type) {
    return transform(getIds(name, type), new IsEmptyFunction());
  }

  /**
   * In this method we prepare all the statements used for accessing Cassandra.
   */
  private void prepareStatements() {
    checkNotNull(session);

    createIdStatement = session.prepare(
        batch(
            insertInto(Tables.ID_TO_NAME)
                .value("label_id", bindMarker())
                .value("type", bindMarker())
                .value("creation_time", bindMarker())
                .value("name", bindMarker())
                .value("meta_description", null),
            insertInto(Tables.NAME_TO_ID)
                .value("name", bindMarker())
                .value("type", bindMarker())
                .value("creation_time", bindMarker())
                .value("label_id", bindMarker())))
        .setConsistencyLevel(ConsistencyLevel.ALL);

    updateNameUidStatement = session.prepare(
        batch(
            update(Tables.ID_TO_NAME)
                .with(set("name", bindMarker()))
                .where(eq("label_id", bindMarker()))
                .and(eq("type", bindMarker()))
                .and(eq("creation_time", bindMarker())),
            delete()
                .from(Tables.NAME_TO_ID)
                .where(eq("name", bindMarker()))
                .and(eq("type", bindMarker()))
                .and(eq("creation_time", bindMarker())),
            insertInto(Tables.NAME_TO_ID)
                .value("name", bindMarker())
                .value("type", bindMarker())
                .value("creation_time", bindMarker())
                .value("label_id", bindMarker())));

    getNameStatement = session.prepare(
        select()
            .all()
            .from(Tables.ID_TO_NAME)
            .where(eq("label_id", bindMarker()))
            .and(eq("type", bindMarker()))
            .limit(2));

    getIdStatement = session.prepare(
        select()
            .all()
            .from(Tables.NAME_TO_ID)
            .where(eq("name", bindMarker()))
            .and(eq("type", bindMarker()))
            .limit(2));

    getMetaStatement = session.prepare(
        select()
            .all()
            .from(Tables.ID_TO_NAME)
            .where(eq("label_id", bindMarker()))
            .and(eq("type", bindMarker()))
            .limit(2));

    updateMetaStatement = session.prepare(
        update(Tables.ID_TO_NAME)
            .with(set("meta_description", bindMarker()))
            .where(eq("label_id", bindMarker()))
            .and(eq("type", bindMarker()))
            .and(eq("creation_time", bindMarker())));
  }
}
