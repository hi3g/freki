package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.storage.StoreTest;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.typesafe.config.Config;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CassandraStoreTest extends StoreTest<CassandraStore> {
  private static final String METRIC_NAME_ONE = "sys";
  private static final String METRIC_NAME_TWO = "cpu0";
  private static final String METRIC_NAME_THREE = "cpu1";

  @Rule
  public final Timeout timeout = Timeout.millis(CassandraTestHelpers.TIMEOUT);

  private Config config;
  private CassandraStoreDescriptor storeDescriptor;
  private Map<String, LabelId> nameUid = new HashMap<>();

  @Before
  public void setUp() throws Exception {
    final CassandraTestComponent cassandraTestComponent = DaggerCassandraTestComponent.create();

    config = cassandraTestComponent.config();

    // It is unfortunate that we can't get a cassandra store directly from dagger but this cast will
    // at least fail hard if it ever is any other store.
    storeDescriptor = (CassandraStoreDescriptor) cassandraTestComponent.storeDescriptor();

    store = storeDescriptor.createStore(config, new MetricRegistry());

    nameUid.put(METRIC_NAME_ONE, store.allocateLabel(
        METRIC_NAME_ONE, LabelType.METRIC).get());

    nameUid.put(METRIC_NAME_TWO, store.allocateLabel(
        METRIC_NAME_TWO, LabelType.METRIC).get());

    nameUid.put(METRIC_NAME_THREE, store.allocateLabel(
        METRIC_NAME_THREE, LabelType.METRIC).get());
  }

  @Test
  public void allocateLabel() throws Exception {
    LabelId newMetricUid = store.allocateLabel("new", LabelType.METRIC).get();
    long maxUid = 0;
    for (LabelId uid : nameUid.values()) {
      maxUid = Math.max(CassandraLabelId.toLong(uid), maxUid);
    }
    Assert.assertEquals(maxUid + 1, CassandraLabelId.toLong(newMetricUid));
  }

  @Test
  public void buildBaseTimeNegativeTime() {
    assertEquals(1434545280000L, CassandraStore.buildBaseTime(1434545416154L));
  }

  @Test
  public void constructor() throws IOException {
    final Cluster cluster = storeDescriptor.createCluster(config);
    final Session session = storeDescriptor.connectTo(cluster);
    assertNotNull(new CassandraStore(cluster, session, Clock.systemDefaultZone()));
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullClock() throws IOException {
    final Cluster cluster = storeDescriptor.createCluster(config);
    final Session session = storeDescriptor.connectTo(cluster);
    new CassandraStore(cluster, session, null);
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullCluster() throws IOException {
    final Cluster cluster = storeDescriptor.createCluster(config);
    final Session session = storeDescriptor.connectTo(cluster);
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
  public void renameId() {
    fail();
    //store.allocateLabel("renamed", new byte[]{0, 0, 4}, LabelType.METRIC);
  }

  @After
  public void tearDown() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
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
}
