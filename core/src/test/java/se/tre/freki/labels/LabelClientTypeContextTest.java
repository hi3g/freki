package se.tre.freki.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.SortedMap;
import javax.inject.Inject;

public final class LabelClientTypeContextTest {
  @Inject Store store;
  @Inject MetricRegistry metrics;
  @Inject EventBus eventBus;

  private LabelClientTypeContext typeContext;
  private final long maxCacheSize = 2000;

  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Before
  public void setUp() throws IOException {
    DaggerTestComponent.create().inject(this);
  }

  @Test(expected = NullPointerException.class)
  public void testCtorNoStore() {
    typeContext = new LabelClientTypeContext(null, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);
  }

  @Test(expected = NullPointerException.class)
  public void testCtorNoType() {
    typeContext = new LabelClientTypeContext(store, null, metrics, eventBus, maxCacheSize);
  }

  @Test(expected = NullPointerException.class)
  public void testCtorNoMetrics() {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, null, eventBus, maxCacheSize);
  }

  @Test(expected = NullPointerException.class)
  public void testCtorNoEventbus() {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, null, maxCacheSize);
  }

  @Test(timeout = TestUtil.TIMEOUT)
  public void getNameSuccessfulLookup() throws Exception {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);

    final LabelId id = store.allocateLabel("foo", LabelType.METRIC).get();

    assertEquals("foo", typeContext.getName(id).get().get());
    // Should be a cache hit ...
    assertEquals("foo", typeContext.getName(id).get().get());

    final SortedMap<String, Counter> counters = metrics.getCounters();
    assertEquals(1, counters.get("uid.cache-hit:kind=metrics").getCount());
    assertEquals(1, counters.get("uid.cache-miss:kind=metrics").getCount());
    assertEquals(2, metrics.getGauges().get("uid.cache-size:kind=metrics").getValue());
  }

  @Test
  public void getNameForNonexistentId() throws Exception {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);
    assertFalse(typeContext.getName(mock(LabelId.class)).get().isPresent());
  }

  @Test(timeout = TestUtil.TIMEOUT)
  public void getIdSuccessfulLookup() throws Exception {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);

    final LabelId id = store.allocateLabel("foo", LabelType.METRIC).get();

    assertEquals(id, typeContext.getId("foo").get().get());
    // Should be a cache hit ...
    assertEquals(id, typeContext.getId("foo").get().get());
    // Should be a cache hit too ...
    assertEquals(id, typeContext.getId("foo").get().get());

    final SortedMap<String, Counter> counters = metrics.getCounters();
    assertEquals(2, counters.get("uid.cache-hit:kind=metrics").getCount());
    assertEquals(1, counters.get("uid.cache-miss:kind=metrics").getCount());
    assertEquals(2, metrics.getGauges().get("uid.cache-size:kind=metrics").getValue());
  }

  @Test
  public void getIdForNonexistentName() throws Exception {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);
    assertFalse(typeContext.getId("foo").get().isPresent());
  }

  @Test
  public void createIdPublishesEventOnSuccess() throws Exception {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);
    typeContext.createId("foo").get();
    verify(eventBus).post(any(LabelCreatedEvent.class));
  }
}
