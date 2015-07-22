package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BaseTimesTest {
  @Test
  public void testBuildBaseTimeNormalizes() {
    final long timestamp = 1434545416154L;
    final long baseTime = 1434542400000L;
    assertEquals(baseTime, BaseTimes.baseTimeFor(timestamp));
  }
}
