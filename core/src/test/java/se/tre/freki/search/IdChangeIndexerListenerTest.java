package se.tre.freki.search;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.storage.MemoryLabelId.randomLabelId;

import se.tre.freki.labels.LabelCreatedEvent;
import se.tre.freki.labels.LabelDeletedEvent;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.storage.Store;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IdChangeIndexerListenerTest {
  private IdChangeIndexerListener idChangeIndexer;

  @Mock private Store store;
  @Mock private SearchPlugin searchPlugin;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    idChangeIndexer = new IdChangeIndexerListener(store, searchPlugin);
  }

  @Test
  public void createdLabelEventIndexesLabelMeta() {
    final LabelId id = mock(LabelId.class);
    LabelMeta labelMeta = LabelMeta.create(id, METRIC, "sys.cpu.0", "Description", 1328140801);
    when(store.getMeta(any(LabelId.class), eq(METRIC))).thenReturn(
        Futures.immediateFuture(Optional.of(labelMeta)));

    when(searchPlugin.indexLabelMeta(labelMeta)).thenAnswer(
        new Answer<ListenableFuture<Void>>() {
          @Override
          public ListenableFuture<Void> answer(final InvocationOnMock invocation)
              throws Throwable {
            return Futures.immediateFuture(null);
          }
        });

    idChangeIndexer.recordLabelCreated(new LabelCreatedEvent(id, "test", LabelType.METRIC));
    verify(searchPlugin).indexLabelMeta(labelMeta);
  }

  @Test
  public void deletedLabelEventRemovesLabelMeta() {
    final LabelId id = randomLabelId();
    final LabelDeletedEvent event =
        new LabelDeletedEvent(id, "test", LabelType.METRIC);

    when(searchPlugin.deleteLabelMeta(id, METRIC)).thenAnswer(new Answer<ListenableFuture<Void>>() {
      @Override
      public ListenableFuture<Void> answer(final InvocationOnMock invocation) throws Throwable {
        return Futures.immediateFuture(null);
      }
    });

    idChangeIndexer.recordLabelDeleted(event);
    verify(searchPlugin).deleteLabelMeta(any(LabelId.class), eq(event.getType()));
  }
}
