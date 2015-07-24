package se.tre.freki.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.tre.freki.labels.LabelType.METRIC;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.labels.LabelException;
import se.tre.freki.labels.LabelId;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.storage.MemoryLabelId;
import se.tre.freki.storage.Store;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;


public class MetaClientTest {

  @Inject Store store;
  @Inject MetaClient metaClient;

  private LabelId sysCpu0;
  private LabelId miss;

  private String name = "sys.cpu.0";
  private String description = "test1";

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);

    sysCpu0 = store.createLabel(name, METRIC).get();
    store.updateMeta(LabelMeta.create(sysCpu0, METRIC, name, description, new Date(0)));
    miss = MemoryLabelId.randomLabelId();
  }

  @Test
  public void testGetLabelMetaMissingMeta() throws Exception {
    try {
      metaClient.getLabelMeta(METRIC, miss).get();
      fail("Should have thrown an exception.");
    } catch (ExecutionException exception) {
      assertTrue(exception.getCause() instanceof LabelException);
    }
  }

  @Test
  public void testGetLabelMeta() throws Exception {
    final Optional<LabelMeta> meta = metaClient.getLabelMeta(METRIC, sysCpu0).get();

    assertEquals(description, meta.get().description());

  }

}
