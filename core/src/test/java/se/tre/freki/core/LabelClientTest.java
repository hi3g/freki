package se.tre.freki.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.labels.LabelException;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.StaticTimeSeriesId;
import se.tre.freki.query.DecoratedTimeSeriesId;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class LabelClientTest {
  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Inject Store store;
  @Inject LabelClient labelClient;

  private LabelId sysCpu0;
  private LabelId web01;

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);

    sysCpu0 = store.createLabel("sys.cpu.0", METRIC).get();
    web01 = store.createLabel("web01", TAGV).get();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createUidInvalidCharacter() {
    labelClient.createId(METRIC, "Not!A:Valid@Name");
  }

  @Test(expected = NullPointerException.class)
  public void createUidNullName() {
    labelClient.createId(METRIC, null);
  }

  @Test(expected = NullPointerException.class)
  public void createUidNullType() {
    labelClient.createId(null, "localhost");
  }

  @Test
  public void createUidTagKey() {
    assertNotNull(labelClient.createId(TAGK, "region"));
  }

  @Test
  public void createUidTagKeyExists() throws Exception {
    try {
      labelClient.createId(TAGV, "web01").get();
      fail("The id should have existed and therefore an exception should have been thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof LabelException);
    }
  }

  @Test
  public void getLabelId() throws Exception {
    assertEquals(sysCpu0, labelClient.getLabelId(METRIC, "sys.cpu.0").get().get());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getLabelIdEmptyName() {
    labelClient.getLabelId(TAGV, "");
  }

  @Test
  public void getLabelIdNoSuchName() throws Exception {
    assertFalse(labelClient.getLabelId(METRIC, "sys.cpu.2").get().isPresent());
  }

  @Test(expected = NullPointerException.class)
  public void getLabelIdNullType() {
    labelClient.getLabelId(null, "sys.cpu.1");
  }

  @Test
  public void getUidName() throws Exception {
    assertEquals("web01", labelClient.getLabelName(TAGV, web01).get().get());
  }

  @Test
  public void getLabelNameNoSuchId() throws Exception {
    assertFalse(labelClient.getLabelName(TAGV, mock(LabelId.class)).get().isPresent());
  }

  @Test(expected = NullPointerException.class)
  public void getUidNameNullType() throws Exception {
    labelClient.getLabelName(null, mock(LabelId.class));
  }

  @Test(expected = NullPointerException.class)
  public void getLabelNameNullId() throws Exception {
    labelClient.getLabelName(TAGV, null);
  }

  @Test
  public void testGetCompulsoryName() throws InterruptedException {
    try {
      labelClient.getCompulsoryName(mock(LabelId.class), METRIC).get();
      fail("#getCompulsoryName should have thrown an exception on a missing name");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof LabelException);
    }
  }

  @Test
  public void testLookupIdTypeMetric() throws Exception {
    final LabelId id = store.createLabel("metric", METRIC).get();
    assertEquals(id, labelClient.lookupId("metric", METRIC).get());
  }

  @Test
  public void testLookupIdTypeTagKey() throws Exception {
    final LabelId id = store.createLabel("tagk", TAGK).get();
    assertEquals(id, labelClient.lookupId("tagk", TAGK).get());
  }

  @Test
  public void testLookupIdTypeTagValue() throws Exception {
    final LabelId id = store.createLabel("tagv", TAGV).get();
    assertEquals(id, labelClient.lookupId("tagv", TAGV).get());
  }

  @Test
  public void testResolveAllExists() throws Exception {
    final LabelId metricId = store.createLabel("metric", METRIC).get();
    final LabelId tagKeyId = store.createLabel("tagKey", TAGK).get();
    final LabelId tagValueId = store.createLabel("tagValue", TAGV).get();
    final StaticTimeSeriesId timeSeriesId = new StaticTimeSeriesId(metricId,
        ImmutableList.of(tagKeyId, tagValueId));

    final DecoratedTimeSeriesId decoratedTimeSeriesId = labelClient.resolve(timeSeriesId).get();
    assertEquals("metric", decoratedTimeSeriesId.metric());
    assertEquals("tagKey", decoratedTimeSeriesId.tags().get(0));
    assertEquals("tagValue", decoratedTimeSeriesId.tags().get(1));
    assertEquals(2, decoratedTimeSeriesId.tags().size());
  }

  @Test
  public void testResolveOneDoesNotExist() throws Exception {
    final LabelId metricId = store.createLabel("metric", METRIC).get();
    final LabelId tagKeyId = store.createLabel("tagKey", TAGK).get();

    // We create an ID with the wrong type so that the resolve can't find it when it tries to look
    // for a tag value.
    final LabelId tagValueId = store.createLabel("tagValue", TAGK).get();

    final StaticTimeSeriesId timeSeriesId = new StaticTimeSeriesId(metricId,
        ImmutableList.of(tagKeyId, tagValueId));

    try {
      labelClient.resolve(timeSeriesId).get();
      fail();
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof LabelException);
    }
  }
}
