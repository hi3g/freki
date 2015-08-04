package se.tre.freki.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static se.tre.freki.labels.LabelType.METRIC;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.plugins.RealTimePublisher;
import se.tre.freki.search.SearchPlugin;
import se.tre.freki.storage.MemoryLabelId;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.google.common.base.Optional;
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
  private LabelId sysCpu1;
  private LabelId miss;

  private String labelOneName = "sys.cpu.0";
  private String labelTwoName = "sys.cpu.1";

  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);
    MockitoAnnotations.initMocks(this);

    sysCpu0 = store.createLabel(labelOneName, LabelType.METRIC).get();
    sysCpu1 = store.createLabel(labelTwoName, LabelType.METRIC).get();

    LabelMeta labelMeta = LabelMeta.create(sysCpu0, LabelType.METRIC, labelOneName, "Description",
        1328140801);
    miss = MemoryLabelId.randomLabelId();

    store.updateMeta(labelMeta);
  }

  @Test
  public void getLabelMetaPresent() throws Exception {
    final LabelMeta meta = metaClient.getLabelMeta(LabelType.METRIC, sysCpu0).get().get();
    Assert.assertEquals(LabelType.METRIC, meta.type());
    assertEquals(labelOneName, meta.name());
    Assert.assertEquals(sysCpu0, meta.identifier());
  }

  @Test
  public void getLabelMetaMetaNotSet() throws Exception {
    final Optional<LabelMeta> optional = metaClient.getLabelMeta(LabelType.METRIC, sysCpu1).get();
    assertFalse(optional.isPresent());
  }

  @Test
  public void testGetLabelMetaMissingMeta() throws Exception {
    Optional<LabelMeta> meta = metaClient.getLabelMeta(METRIC, miss).get();
    assertFalse(meta.isPresent());
  }
}
