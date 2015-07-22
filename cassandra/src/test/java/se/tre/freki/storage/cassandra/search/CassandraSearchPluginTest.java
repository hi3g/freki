package se.tre.freki.storage.cassandra.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.storage.cassandra.CassandraLabelId;
import se.tre.freki.storage.cassandra.CassandraStore;
import se.tre.freki.storage.cassandra.CassandraStoreDescriptor;
import se.tre.freki.storage.cassandra.CassandraTestComponent;
import se.tre.freki.storage.cassandra.CassandraTestHelpers;
import se.tre.freki.storage.cassandra.DaggerCassandraTestComponent;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class CassandraSearchPluginTest {
  private CassandraStoreDescriptor storeDescriptor;
  private Config config;
  private CassandraStore store;
  private CassandraSearchPlugin searchPlugin;


  @Before
  public void setUp() throws Exception {
    final CassandraTestComponent cassandraTestComponent = DaggerCassandraTestComponent.create();
    config = cassandraTestComponent.config();
    // It is unfortunate that we can't get a cassandra store directly from dagger but this cast will
    // at least fail hard if it ever is any other store.
    storeDescriptor = (CassandraStoreDescriptor) cassandraTestComponent.storeDescriptor();

    store = storeDescriptor.createStore(config, new MetricRegistry());
    searchPlugin = new CassandraSearchPlugin(store);
  }

  @After
  public void tearDown() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
  }

  @Test
  public void testNGram() throws Exception {
    LabelMeta meta = LabelMeta.create(CassandraLabelId.fromLong(23L), LabelType.METRIC, "jungle",
        "and its description", 0L);
    LabelMeta meta2 = LabelMeta.create(CassandraLabelId.fromLong(22L), LabelType.METRIC, "junglar",
        "and its description", 0L);

    searchPlugin.indexLabelMeta(meta2);
    searchPlugin.indexLabelMeta(meta);
  }

  @Test
  public void testFetchLabelIdsForEmptyString() throws Exception {
    List<ResultSetFuture> result = searchPlugin.fetchLabelIds(ImmutableSet.<String>of().iterator());
    assertTrue(result.isEmpty());
  }

  @Test
  public void testLabelIdsForExistingNgram() throws Exception {
    LabelMeta meta = LabelMeta.create(CassandraLabelId.fromLong(23L), LabelType.METRIC, "jungle",
        "and its description", 0L);
    searchPlugin.indexLabelMeta(meta).get();

    final NGramGenerator ngram = new NGramGenerator("jungle");
    final List<ResultSetFuture> result = searchPlugin.fetchLabelIds(ngram);

    for (final ResultSetFuture resultSetFuture : result) {
      ResultSet rs = resultSetFuture.get();
      for (final Row r : rs) {
        assertEquals(23L, r.getLong("label_id"));
        assertEquals(LabelType.METRIC.toValue(), r.getString(1));
      }
    }
  }
}
