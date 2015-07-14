package se.tre.freki.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import javax.inject.Inject;

public class WildcardIdLookupStrategyTest {
  @Inject Store client;
  @Inject MetricRegistry metricRegistry;
  @Inject EventBus eventBus;

  private LabelClientTypeContext uid;
  private IdLookupStrategy lookupStrategy;

  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Before
  public void setUp() throws IOException {
    DaggerTestComponent.create().inject(this);

    uid = new LabelClientTypeContext(client, LabelType.METRIC, metricRegistry, eventBus);
    lookupStrategy = new IdLookupStrategy.WildcardIdLookupStrategy();
  }

  @Test
  public void testResolveIdWildcardNull() throws Exception {
    assertNull(lookupStrategy.getId(uid, null).get());
  }

  @Test
  public void testResolveIdWildcardEmpty() throws Exception {
    assertNull(lookupStrategy.getId(uid, "").get());
  }

  @Test
  public void testResolveIdWildcardStar() throws Exception {
    assertNull(lookupStrategy.getId(uid, "*").get());
  }

  @Test(timeout = TestUtil.TIMEOUT)
  public void testResolveIdGetsId() throws Exception {
    LabelId id = client.allocateLabel("nameexists", LabelType.METRIC).get();
    assertEquals(id, lookupStrategy.getId(uid, "nameexists").get());
  }

  @Test(expected = LabelException.class)
  public void testResolveIdGetsMissingId() throws Exception {
    lookupStrategy.getId(uid, "nosuchname").get();
  }
}
