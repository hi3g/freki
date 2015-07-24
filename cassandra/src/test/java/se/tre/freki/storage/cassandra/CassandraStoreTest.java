package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.storage.StoreTest;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.typesafe.config.Config;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

public class CassandraStoreTest extends StoreTest<CassandraStore> {
  @Rule
  public final Timeout timeout = Timeout.millis(CassandraTestHelpers.TIMEOUT);

  private Config config;
  private CassandraStoreDescriptor storeDescriptor;

  @Override
  protected CassandraStore buildStore() {
    final CassandraTestComponent cassandraTestComponent = DaggerCassandraTestComponent.create();

    config = cassandraTestComponent.config();

    // It is unfortunate that we can't get a cassandra store directly from dagger but this cast will
    // at least fail hard if it ever is any other store.
    storeDescriptor = (CassandraStoreDescriptor) cassandraTestComponent.storeDescriptor();

    return storeDescriptor.createStore(config, new MetricRegistry());
  }

  @After
  public void tearDown() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
  }

  @Test
  public void constructor() throws IOException {
    final String keyspace = config.getString("freki.storage.cassandra.keyspace");
    final Cluster cluster = storeDescriptor.createCluster(config);
    final Session session = storeDescriptor.connectTo(cluster, keyspace);
    assertNotNull(new CassandraStore(cluster, session, Clock.systemDefaultZone()));
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullClock() throws IOException {
    final String keyspace = config.getString("freki.storage.cassandra.keyspace");
    final Cluster cluster = storeDescriptor.createCluster(config);
    final Session session = storeDescriptor.connectTo(cluster, keyspace);
    new CassandraStore(cluster, session, null);
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullCluster() throws IOException {
    final String keyspace = config.getString("freki.storage.cassandra.keyspace");
    final Cluster cluster = storeDescriptor.createCluster(config);
    final Session session = storeDescriptor.connectTo(cluster, keyspace);
    new CassandraStore(null, session, Clock.systemDefaultZone());
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullSession() throws IOException {
    final Cluster cluster = storeDescriptor.createCluster(config);
    new CassandraStore(cluster, null, Clock.systemDefaultZone());
  }

  @Override
  protected LabelId missingLabelId() {
    return CassandraLabelId.fromLong(15L);
  }

  @Test
  public void testCreateId() throws Exception {
    final String doesNotExistName = "does.not.exist";
    final long doesNotExistId = 10L;

    final LabelId newId = store.createId(doesNotExistId, doesNotExistName, LabelType.METRIC).get();
    assertEquals(doesNotExistName, store.getName(newId, LabelType.METRIC).get().get());
  }

  @Test
  public void testGetIdsMissingEmptyList() throws Exception {
    final List<LabelId> missing = store.getIds("missing", LabelType.TAGK).get();
    assertEquals(0, missing.size());
  }

  @Test
  public void testGetIdsReturnsTwoOrder() throws Exception {
    final LabelId firstId = CassandraLabelId.fromLong(10L);
    final LabelId secondId = CassandraLabelId.fromLong(11L);
    final String name = "labelName";

    store.createId(CassandraLabelId.toLong(firstId), name, LabelType.METRIC).get();
    store.createId(CassandraLabelId.toLong(secondId), name, LabelType.METRIC).get();
    final List<LabelId> ids = store.getIds(name, LabelType.METRIC).get();

    assertEquals(firstId, ids.get(0));
    assertEquals(secondId, ids.get(1));
  }

  @Test
  public void testGetNamesMissingEmptyList() throws Exception {
    final List<String> missing = store.getNames(CassandraLabelId.fromLong(10L), LabelType.TAGK)
        .get();
    assertEquals(0, missing.size());
  }

  @Test
  public void testGetNamesReturnsTwoOrder() throws Exception {
    final String firstName = "firstName";
    final String secondName = "secondName";
    final long duplicateId = 10L;

    store.createId(duplicateId, firstName, LabelType.METRIC).get();
    store.createId(duplicateId, secondName, LabelType.METRIC).get();
    final List<String> names = store.getNames(CassandraLabelId.fromLong(duplicateId),
        LabelType.METRIC).get();

    assertEquals(firstName, names.get(0));
    assertEquals(secondName, names.get(1));
  }

  @Test
  public void testRenameIdNotFound() {
    try {
      store.renameLabel("anyUnusedName", missingLabelId(), LabelType.TAGK).get().get();
      fail("Should have thrown an Exception");
    } catch (Exception exception) {
      assertTrue(exception.getCause() instanceof IndexOutOfBoundsException);
    }
  }
}
