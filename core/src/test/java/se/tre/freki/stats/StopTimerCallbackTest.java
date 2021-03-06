package se.tre.freki.stats;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;

public class StopTimerCallbackTest {
  @Test
  public void testStopOnCallsStopOnTimer() throws Exception {
    ListenableFuture<Object> future = Futures.immediateFuture(null);
    Timer.Context timerContext = mock(Timer.Context.class);
    StopTimerCallback.stopOn(timerContext, future);
    verify(timerContext, times(1)).stop();
  }
}
