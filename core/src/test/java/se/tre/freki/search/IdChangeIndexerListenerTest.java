package se.tre.freki.search;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.tre.freki.labels.LabelType.METRIC;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.labels.LabelCreatedEvent;
import se.tre.freki.labels.LabelDeletedEvent;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.storage.Store;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;

public class IdChangeIndexerListenerTest {
  @Inject EventBus idEventBus;

  @Mock private Store store;
  @Mock private SearchPlugin searchPlugin;

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);
    MockitoAnnotations.initMocks(this);

    IdChangeIndexerListener idChangeIndexer = new IdChangeIndexerListener(store, searchPlugin);
    idEventBus.register(idChangeIndexer);
  }

  @Test
  public void createdLabelEventIndexesLabelMeta() {
    final LabelId id = mock(LabelId.class);
    LabelMeta labelMeta = LabelMeta.create(id, METRIC, "sys.cpu.0", "Description", 1328140801);
    when(store.getMeta(any(LabelId.class), METRIC)).thenReturn(Futures.immediateFuture(labelMeta));

    idEventBus.post(new LabelCreatedEvent(id, "test", LabelType.METRIC));
    verify(searchPlugin).indexLabelMeta(labelMeta);
  }

  @Test
  public void deletedLabelEventRemovesLabelMeta() {
    final LabelDeletedEvent event =
        new LabelDeletedEvent(mock(LabelId.class), "test", LabelType.METRIC);
    idEventBus.post(event);
    verify(searchPlugin).deleteLabelMeta(any(LabelId.class), event.getType());
  }
}
