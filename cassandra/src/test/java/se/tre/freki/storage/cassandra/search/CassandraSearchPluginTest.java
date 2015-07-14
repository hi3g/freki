package se.tre.freki.storage.cassandra.search;

import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.storage.cassandra.CassandraLabelId;
import se.tre.freki.storage.cassandra.CassandraStore;
import se.tre.freki.storage.cassandra.CassandraStoreDescriptor;
import se.tre.freki.storage.cassandra.CassandraTestComponent;
import se.tre.freki.storage.cassandra.CassandraTestHelpers;
import se.tre.freki.storage.cassandra.DaggerCassandraTestComponent;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Session;
import com.typesafe.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CassandraSearchPluginTest {
  private CassandraStoreDescriptor storeDescriptor;
  private Config config;
  private CassandraStore store;
  private Session session;
  private CassandraSearchPlugin searchPlugin;
  private String keyspace;


  @Before
  public void setUp() throws Exception {
    final CassandraTestComponent cassandraTestComponent = DaggerCassandraTestComponent.create();
    config = cassandraTestComponent.config();
    keyspace = config.getString("freki.storage.cassandra.keyspace");
    // It is unfortunate that we can't get a cassandra store directly from dagger but this cast will
    // at least fail hard if it ever is any other store.
    storeDescriptor = (CassandraStoreDescriptor) cassandraTestComponent.storeDescriptor();

    store = storeDescriptor.createStore(config, new MetricRegistry());
    session = store.getSession();
    searchPlugin = new CassandraSearchPlugin(store, keyspace);
  }

  @After
  public void tearDown() throws Exception {
    keyspace = config.getString("freki.storage.cassandra.keyspace");
    CassandraTestHelpers.truncate(store.getSession(), keyspace);
  }

  @Test
  public void testNGram() throws Exception {
    LabelMeta meta = LabelMeta.create(CassandraLabelId.fromLong(23L), LabelType.METRIC, "jungle",
        "and its description", 0L);

    searchPlugin.indexLabelMeta(meta);
  }

  @Test
  public void testSearchLabel() throws Exception {
    searchPlugin.findLabels("jungle");
  }
}
