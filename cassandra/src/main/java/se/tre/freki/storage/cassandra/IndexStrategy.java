package se.tre.freki.storage.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.google.common.base.Preconditions.checkNotNull;
import static se.tre.freki.storage.cassandra.CassandraLabelId.toLong;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

/**
 * An indexing strategy defines the behavior for how time series IDs should be indexed. The
 * interesting part is not how they are indexed though, but when which strategy is used.
 */
interface IndexStrategy {
  /**
   * Write the time series ID to the database so that it will be resolved against the provided
   * metric and tags.
   *
   * @param metric The metric the time series ID represents
   * @param tags The tags the time series ID represents
   * @param timeSeriresId The internal Cassandra representation of a time series ID
   */
  void indexTimeseriesId(final LabelId metric,
                         final List<LabelId> tags,
                         final ByteBuffer timeSeriresId);

  /**
   * An indexing strategy which does nothing and therefore should not have any penalties.
   */
  class NoOpIndexingStrategy implements IndexStrategy {
    @Override
    public void indexTimeseriesId(final LabelId metric,
                                  final List<LabelId> tags,
                                  final ByteBuffer timeSeriresId) {
    }
  }

  /**
   * An indexing strategy which writes the time series ID to the database. Due to the number of
   * database calls this makes it is a bit heavy-weight.
   */
  class IndexingStrategy implements IndexStrategy {
    private final Session session;
    private final PreparedStatement insertTagsStatement;

    public IndexingStrategy(final Session session) {
      this.session = checkNotNull(session);

      insertTagsStatement = session.prepare(
          insertInto(Tables.TS_INVERTED_INDEX)
              .value("label_id", bindMarker())
              .value("type", bindMarker())
              .value("timeseries_id", bindMarker()));
    }

    @Override
    public void indexTimeseriesId(final LabelId metric,
                                  final List<LabelId> tags,
                                  final ByteBuffer timeSeriresId) {
      session.executeAsync(insertTagsStatement.bind()
          .setLong(0, toLong(metric))
          .setString(1, LabelType.METRIC.toValue())
          .setBytesUnsafe(2, timeSeriresId));

      final Iterator<LabelId> tagIterator = tags.iterator();

      while (tagIterator.hasNext()) {
        session.executeAsync(insertTagsStatement.bind()
            .setLong(0, toLong(tagIterator.next()))
            .setString(1, LabelType.TAGK.toValue())
            .setBytesUnsafe(2, timeSeriresId));

        session.executeAsync(insertTagsStatement.bind()
            .setLong(0, toLong(tagIterator.next()))
            .setString(1, LabelType.TAGV.toValue())
            .setBytesUnsafe(2, timeSeriresId));
      }
    }
  }
}
