package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.storage.StoreTest;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.base.Optional;
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
  private static final String TAGK_NAME_ONE = "host";
  private static final String TAGV_NAME_ONE = "127.0.0.1";

  private static LabelId TAGK_UID_ONE;
  private static LabelId TAGV_UID_ONE;

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

    TAGK_UID_ONE = store.allocateLabel(TAGK_NAME_ONE, LabelType.TAGK).get();
    TAGV_UID_ONE = store.allocateLabel(TAGV_NAME_ONE, LabelType.TAGV).get();


    /*
    store.addPoint(TSUID_ONE, new byte[]{'d', '1'}, 1356998400, (short) 'a');
    store.addPoint(TSUID_ONE, new byte[]{'d', '2'}, 1356998401, (short) 'b');
    store.addPoint(TSUID_ONE, new byte[]{'d', '2'}, 1357002078, (short) 'b');

    store.addPoint(TSUID_TWO, new byte[]{'d', '3'}, 1356998400, (short) 'b');
    */
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

  /*
  @Test
  public void addPoint() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
    //'001001001', 1356998400, 1356998400, 'a', 'data1'
    store.addPoint(
            new byte[]{0, 0, 1, 0, 0, 1, 0, 0, 1},
            new byte[]{'v', 'a', 'l', 'u', 'e', '1'},
            (long) 1356998400,
            (short) 47).get(TIMEOUT);
  }

  @Test(expected = NullPointerException.class)
  public void addPointNull() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
    store.addPoint(
            null,
            new byte[]{'v', 'a', 'l', 'u', 'e', '1'},
            (long) 1356998400,
            (short) 47).get(TIMEOUT);

  }

  @Test(expected = IllegalArgumentException.class)
  public void addPointEmpty() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
    store.addPoint(
            new byte[]{},
            new byte[]{'v', 'a', 'l', 'u', 'e', '1'},
            (long) 1356998400,
            (short) 47).get(TIMEOUT);

  }

  @Test(expected = IllegalArgumentException.class)
  public void addPointTooShort() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
    store.addPoint(
            new byte[]{0, 0, 1, 0, 0, 1, 0, 0},
            new byte[]{'v', 'a', 'l', 'u', 'e', '1'},
            (long) 1356998400,
            (short) 47).get(TIMEOUT);
  }
  */

  @Test
  public void getId() throws Exception {
    validateValidId(METRIC_NAME_ONE, LabelType.METRIC);
    validateValidId(METRIC_NAME_TWO, LabelType.METRIC);
    validateValidId(METRIC_NAME_THREE, LabelType.METRIC);
    validateInvalidId("Missing", LabelType.METRIC);
    validateValidId(TAGK_NAME_ONE, LabelType.TAGK);
    validateValidId(TAGV_NAME_ONE, LabelType.TAGV);
  }

  @Test
  public void getName() throws Exception {
    validateValidName(METRIC_NAME_ONE, LabelType.METRIC);
    validateValidName(METRIC_NAME_TWO, LabelType.METRIC);
    validateValidName(METRIC_NAME_THREE, LabelType.METRIC);
    validateInvalidName(mock(LabelId.class), LabelType.METRIC);
    validateValidName(TAGK_NAME_ONE, LabelType.TAGK);
    validateValidName(TAGV_NAME_ONE, LabelType.TAGK);
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

  private void validateInvalidId(final String name, final LabelType type)
      throws Exception {
    Optional<LabelId> value = store.getId(name, type).get();
    assertFalse(value.isPresent());
  }

  private void validateInvalidName(final LabelId uid, final LabelType type)
      throws Exception {
    Optional<String> value = store.getName(uid, type).get();
    assertFalse(value.isPresent());
  }

  private void validateValidId(final String name, final LabelType type)
      throws Exception {
    Optional<LabelId> value = store.getId(name, type).get();

    assertTrue(value.isPresent());
    assertEquals(nameUid.get(name), value.get());
  }

  private void validateValidName(final String name, final LabelType type)
      throws Exception {
    Optional<String> value = store.getName(nameUid.get(name), type).get();

    assertTrue(value.isPresent());
    assertEquals(name, value.get());
  }
}
