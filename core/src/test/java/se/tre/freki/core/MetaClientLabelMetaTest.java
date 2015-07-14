package se.tre.freki.core;

import static org.junit.Assert.assertEquals;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.plugins.RealTimePublisher;
import se.tre.freki.search.SearchPlugin;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.google.common.eventbus.EventBus;
import com.typesafe.config.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;

public class MetaClientLabelMetaTest {
  @Inject Config config;
  @Inject EventBus idEventBus;
  @Inject Store store;

  @Inject LabelClient labelClient;
  @Inject MetaClient metaClient;

  @Inject RealTimePublisher realtimePublisher;

  @Mock private SearchPlugin searchPlugin;

  private LabelId sysCpu0;
  private LabelId sysCpu2;

  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);
    MockitoAnnotations.initMocks(this);

    sysCpu0 = store.allocateLabel("sys.cpu.0", LabelType.METRIC).get();
    sysCpu2 = store.allocateLabel("sys.cpu.2", LabelType.METRIC).get();

    LabelMeta labelMeta = LabelMeta.create(sysCpu0, LabelType.METRIC, "sys.cpu.0", "Description",
        1328140801);

    store.updateMeta(labelMeta);
  }

  @Test
  public void getLabelMeta() throws Exception {
    final LabelMeta meta = metaClient.getLabelMeta(LabelType.METRIC, sysCpu2).get();
    Assert.assertEquals(LabelType.METRIC, meta.type());
    assertEquals("sys.cpu.2", meta.name());
    Assert.assertEquals(sysCpu2, meta.identifier());
  }
}
