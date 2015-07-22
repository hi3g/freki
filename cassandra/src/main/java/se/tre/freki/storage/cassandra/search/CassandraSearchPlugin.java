package se.tre.freki.storage.cassandra.search;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.successfulAsList;
import static com.google.common.util.concurrent.Futures.transform;

import se.tre.freki.BuildData;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.Annotation;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.search.SearchPlugin;
import se.tre.freki.storage.cassandra.CassandraLabelId;
import se.tre.freki.storage.cassandra.CassandraStore;
import se.tre.freki.storage.cassandra.Tables;
import se.tre.freki.storage.cassandra.functions.ToVoidFunction;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

public class CassandraSearchPlugin extends SearchPlugin {
  private static final Logger LOG = LoggerFactory.getLogger(CassandraSearchPlugin.class);
  private final Session session;
  private final CassandraStore cassandraStore;
  private final PreparedStatement createNGramStatement;
  private final PreparedStatement selectNGramStatment;

  /**
   * Creates a new instance of a {@link CassandraSearchPlugin}.
   *
   * @param cassandraStore an instance of a {@link CassandraStore}.
   */
  public CassandraSearchPlugin(final CassandraStore cassandraStore) {
    this.session = checkNotNull(cassandraStore.getSession());
    this.cassandraStore = checkNotNull(cassandraStore);
    createNGramStatement = session.prepare(
        insertInto(Tables.LABEL_SEARCH_INDEX)
            .value("ngram", bindMarker())
            .value("type", bindMarker())
            .value("label_id", bindMarker()));

    selectNGramStatment = session.prepare(
        select()
            .all()
            .from(Tables.LABEL_SEARCH_INDEX)
            .where(eq("ngram", bindMarker()))
            .and(eq("type", bindMarker()))
            .limit(5));
  }

  @Override
  public String version() {
    return BuildData.version();
  }

  @Override
  public void close() throws IOException {

  }

  @Nonnull
  @Override
  public ListenableFuture<Void> indexLabelMeta(final LabelMeta meta) {

    final NGramGenerator ngram = new NGramGenerator(meta.name());

    return writeNGramsIndex(ngram, (CassandraLabelId) meta.identifier(), meta.type());
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteLabelMeta(final LabelId id,
                                                final LabelType type) {
    return immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> indexAnnotation(final Annotation note) {
    return immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteAnnotation(final Annotation note) {
    return immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Iterable<LabelMeta>> findLabels(final String query) {
    final NGramGenerator ngrams = new NGramGenerator(query);
    final List<ResultSetFuture> resultSets = fetchLabelIds(ngrams);

    return transform(
        successfulAsList(resultSets),
        new AsyncFunction<List<ResultSet>, Iterable<LabelMeta>>() {
          @Override
          public ListenableFuture<Iterable<LabelMeta>> apply(final List<ResultSet> resultSets) {
            return transform(fetchLabelMetas(resultSets),
                new Function<Iterable<LabelMeta>, Iterable<LabelMeta>>() {
                  @Override
                  public Iterable<LabelMeta> apply(final Iterable<LabelMeta> input) {
                    return input;
                  }
                });
          }
        });
  }

  /**
   * Takes a {@link List} of {@link ResultSetFuture}s and for each label id in the {@link
   * ResultSet}s gets the corresponding {@link LabelMeta}.
   *
   * @param resultSets A List of ResultSets
   * @return A {@link ListenableFuture} with a {@link List} of {@link LabelMeta}s.
   */
  protected ListenableFuture<? extends Iterable<LabelMeta>> fetchLabelMetas(
      final List<ResultSet> resultSets) {

    final List<ListenableFuture<LabelMeta>> metas = new ArrayList<>();

    for (final ResultSet resultSet : resultSets) {
      for (final Row row : resultSet) {
        final CassandraLabelId id = CassandraLabelId.fromLong(row.getLong("label_id"));
        metas.add(cassandraStore.getMeta(id, id.type()));
      }
    }

    return allAsList(metas);
  }

  /**
   * Takes a {@link Iterator} of {@link String}s and queries cassandra for label ids matching each
   * n-gram in the {@link Iterator} of {@link String}s supplied.
   *
   * @param ngrams A Iterator of Strings that we want to find the corresponding label ids for.
   * @return A {@link ResultSetFuture} with the corresponding query {@link ResultSet}.
   */
  protected List<ResultSetFuture> fetchLabelIds(final Iterator<String> ngrams) {
    final List<ResultSetFuture> resultSets = new ArrayList<>();

    while (ngrams.hasNext()) {
      final String ngram = ngrams.next();
      if (ngram.isEmpty()) {
        return resultSets;
      }
      final ResultSetFuture metricResult = labelIdQuery(ngram, LabelType.METRIC);
      final ResultSetFuture tagkResult = labelIdQuery(ngram, LabelType.TAGK);
      final ResultSetFuture tagvResult = labelIdQuery(ngram, LabelType.TAGV);

      resultSets.add(metricResult);
      resultSets.add(tagkResult);
      resultSets.add(tagvResult);
    }
    return resultSets;
  }

  /**
   * Takes a n-gram and a {@link LabelType} and queries cassandra for the {@link CassandraLabelId}s
   * that matches the n-gram and type and returns this a {@link ResultSetFuture}.
   *
   * @param ngram an n-gram used to lookup {@link CassandraLabelId}.
   * @param type The {@link LabelType} of the label we look for.
   * @return A {@link ResultSetFuture} containing a {@link ResultSet} for the queried n-gram and
   * {@link LabelType}.
   */
  protected ResultSetFuture labelIdQuery(final String ngram, final LabelType type) {

    final ResultSetFuture result = session.executeAsync(selectNGramStatment.bind()
        .setString(0, ngram)
        .setString(1, type.toValue()));

    addCallback(result, new FutureCallback<ResultSet>() {
      @Override
      public void onSuccess(final ResultSet result) {
        //cassandra call succeeded
      }

      @Override
      public void onFailure(final Throwable throwable) {
        LOG.error("Search for n-gram {} of type {} failed", ngram, type, throwable);
      }
    });

    return result;
  }

  /**
   * Takes an {@link NGramGenerator} generated from a label IDs name and creates an index for this
   * label ID in the database.
   *
   * @param ngrams An iterator over a strings 3-grams.
   * @param labelId A labels unique ID.
   * @param type A labels type.
   * @return A future that indicates the completion of the request or an error.
   */
  private ListenableFuture<Void> writeNGramsIndex(final NGramGenerator ngrams,
                                                  final CassandraLabelId labelId,
                                                  final LabelType type) {
    final List<ResultSetFuture> list = new ArrayList<>();

    while (ngrams.hasNext()) {
      final String nGram = ngrams.next();
      final ResultSetFuture result = session.executeAsync(createNGramStatement.bind()
          .setString(0, nGram)
          .setString(1, type.toValue())
          .setLong(2, CassandraLabelId.toLong(labelId)));
      addCallback(result, new FutureCallback<ResultSet>() {

        @Override
        public void onSuccess(final ResultSet result) {
          //If nothing went wrong we are fine
        }

        @Override
        public void onFailure(final Throwable throwable) {
          LOG.error("Write of n-gram {} with id {} of type {} failed", nGram, labelId, type,
              throwable);
        }
      });
      list.add(result);
    }
    return transform(allAsList(list), new ToVoidFunction());
  }
}
