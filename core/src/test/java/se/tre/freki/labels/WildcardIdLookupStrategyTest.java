package se.tre.freki.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class WildcardIdLookupStrategyTest {
  @Inject Store client;
  @Inject EventBus eventBus;

  private LabelClientTypeContext uid;
  private IdLookupStrategy lookupStrategy;

  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Before
  public void setUp() throws IOException {
    DaggerTestComponent.create().inject(this);

    final long maxCacheSize = 2000;
    uid = new LabelClientTypeContext(client, LabelType.METRIC, eventBus, maxCacheSize);

    lookupStrategy = new IdLookupStrategy.WildcardIdLookupStrategy();
  }

  @Test
  public void testResolveIdWildcardStar() throws Exception {
    assertNull(lookupStrategy.getId(uid, "*").get());
  }

  @Test(timeout = TestUtil.TIMEOUT)
  public void testResolveIdGetsId() throws Exception {
    LabelId id = client.createLabel("nameexists", LabelType.METRIC).get();
    assertEquals(id, lookupStrategy.getId(uid, "nameexists").get());
  }

  @Test
  public void testResolveIdGetsMissingId() throws Exception {
    try {
      lookupStrategy.getId(uid, "nosuchname").get();
      fail("The wildcard lookup strategy should fail on missing names");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof LabelException);
    }
  }
}
