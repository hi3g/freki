package se.tre.freki.labels;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.storage.MemoryLabelId.randomLabelId;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import javax.inject.Inject;

public final class LabelClientTypeContextTest {
  @Inject Store store;
  @Inject MetricRegistry metrics;

  @Mock EventBus eventBus;

  private LabelClientTypeContext typeContext;
  private final long maxCacheSize = 2000;

  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    DaggerTestComponent.create().inject(this);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorNoStore() {
    typeContext = new LabelClientTypeContext(null, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorNoType() {
    typeContext = new LabelClientTypeContext(store, null, metrics, eventBus, maxCacheSize);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorNoMetrics() {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, null, eventBus, maxCacheSize);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorNoEventbus() {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, null, maxCacheSize);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNegativeMaxSize() {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, eventBus, -5);
  }

  @Test
  public void testGetNameFromStorePresent() throws Exception {
    store = mock(Store.class);
    final LabelId id = randomLabelId();

    typeContext = new LabelClientTypeContext(store, TAGK, metrics, eventBus, maxCacheSize);

    when(store.getName(id, TAGK)).thenAnswer(
        new Answer<ListenableFuture<Optional<String>>>() {
          @Override
          public ListenableFuture<Optional<String>> answer(final InvocationOnMock invocation)
              throws Throwable {
            return immediateFuture(Optional.of("name"));
          }
        });

    // The first call should hit the store
    assertEquals("name", typeContext.getName(id).get().get());
    verify(store, times(1)).getName(id, TAGK);

    // On the second call the store should still only have been
    // hit once since the name should now be cached.
    assertEquals("name", typeContext.getName(id).get().get());
    verify(store, times(1)).getName(id, TAGK);
  }

  @Test
  public void testGetNameAbsent() throws Exception {
    typeContext = new LabelClientTypeContext(store, LabelType.METRIC, metrics, eventBus,
        maxCacheSize);
    assertFalse(typeContext.getName(mock(LabelId.class)).get().isPresent());
  }

  @Test
  public void testGetIdFromStorePresent() throws Exception {
    store = mock(Store.class);
    final LabelId id = randomLabelId();

    typeContext = new LabelClientTypeContext(store, TAGK, metrics, eventBus, maxCacheSize);

    when(store.getId("name", TAGK)).thenAnswer(
        new Answer<ListenableFuture<Optional<LabelId>>>() {
          @Override
          public ListenableFuture<Optional<LabelId>> answer(final InvocationOnMock invocation)
              throws Throwable {
            return immediateFuture(Optional.of(id));
          }
        });

    // The first call should hit the store
    assertEquals(id, typeContext.getId("name").get().get());
    verify(store, times(1)).getId("name", TAGK);

    // On the second call the store should still only have been
    // hit once since the name should now be cached.
    assertEquals(id, typeContext.getId("name").get().get());
    verify(store, times(1)).getId("name", TAGK);
  }

  @Test
  public void testGetIdAbsent() throws Exception {
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
