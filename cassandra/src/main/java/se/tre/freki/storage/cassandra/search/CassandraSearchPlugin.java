package se.tre.freki.storage.cassandra.search;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.allAsList;

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
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class CassandraSearchPlugin extends SearchPlugin {
  private static final Logger LOG = LoggerFactory.getLogger(CassandraSearchPlugin.class);
  private final Session session;
  private final CassandraStore cassandraStore;
  private final PreparedStatement createNGramStatement;
  private final PreparedStatement selectNGramStatment;

  /**
   * Creates a new instance of a CassandraSearchPlugin.
   *
   * @param cassandraStore an instance of a CassandraStore
   */
  public CassandraSearchPlugin(final CassandraStore cassandraStore) {
    this.session = checkNotNull(cassandraStore.getSession());
    this.cassandraStore = checkNotNull(cassandraStore);
    createNGramStatement = session.prepare(
        insertInto(Tables.KEYSPACE, Tables.LABEL_SEARCH_INDEX)
            .value("ngram", bindMarker())
            .value("type", bindMarker())
            .value("label_id", bindMarker()));

    selectNGramStatment = session.prepare(
        select()
            .all()
            .from(Tables.KEYSPACE, Tables.LABEL_SEARCH_INDEX)
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

    ListenableFuture<Void> result = writeNGramsIndex(ngram, (CassandraLabelId) meta.identifier(),
        meta.type());

    return result;
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteLabelMeta(final LabelId id,
                                                final LabelType type) {
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> indexAnnotation(final Annotation note) {
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteAnnotation(final Annotation note) {
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Iterable<LabelMeta>> findLabels(final String query) {
    return Futures.<Iterable<LabelMeta>>immediateFuture(ImmutableSet.<LabelMeta>of());
  }

  /**
   * Takes an {@link NGramGenerator} generated from a labelIds name and creates an index for this
   * labelId in the database.
   *
   * @param ngrams An iterator over a strings 3-grams.
   * @param labelId A labels unique ID.
   * @param type A labels type.
   * @return A future that indicates the completion of the request or an error.
   */
  private ListenableFuture<Void> writeNGramsIndex(final NGramGenerator ngrams,
                                                  final CassandraLabelId labelId,
                                                  final LabelType type) {
    List<ResultSetFuture> list = new ArrayList<>();

    while (ngrams.hasNext()) {
      final String nGram = ngrams.next();
      ResultSetFuture result = session.executeAsync(createNGramStatement.bind()
          .setString(0, nGram)
          .setString(1, type.toValue())
          .setLong(2, CassandraLabelId.toLong(labelId)));
      Futures.addCallback(result, new FutureCallback<ResultSet>() {

        @Override
        public void onSuccess(final ResultSet result) {
          //If nothing went wrong we are fine
        }

        @Override
        public void onFailure(final Throwable throwable) {
          LOG.error("Write of ngram {} with id {} of type {}", nGram, labelId, type );
        }
      });
      list.add(result);
    }
    return Futures.transform(allAsList(list), new ToVoidFunction());
  }
}
