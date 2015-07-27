package se.tre.freki.labels;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import se.tre.freki.storage.MemoryLabelId;
import se.tre.freki.utils.TestUtil;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;

public class CreatingIdLookupStrategyTest {
  private IdLookupStrategy lookupStrategy;

  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Before
  public void setUp() throws IOException {
    lookupStrategy = new IdLookupStrategy.CreatingIdLookupStrategy();
  }

  @Test
  public void testGetIdForAbsentIdCreated() throws Exception {
    final LabelClientTypeContext typeContext = mock(LabelClientTypeContext.class);
    when(typeContext.getId(eq("noSuchName"))).thenReturn(immediateFuture(Optional.absent()));
    lookupStrategy.getId(typeContext, "noSuchName");
    verify(typeContext).createId("noSuchName");
  }

  @Test
  public void testGetIdForPresentIdNotCreated() throws Exception {
    final LabelClientTypeContext typeContext = mock(LabelClientTypeContext.class);
    when(typeContext.getId(eq("presentName")))
        .thenReturn(immediateFuture(Optional.of(MemoryLabelId.randomLabelId())));
    lookupStrategy.getId(typeContext, "presentName");
    verify(typeContext, times(0)).createId("presentName");
  }
}
