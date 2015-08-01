package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static se.tre.freki.storage.cassandra.BaseTimes.baseTimesBetween;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

public class BaseTimesTest {
  @Test
  public void testBuildBaseTimeNormalizes() {
    final long timestamp = 1434545416154L;
    final long baseTime = 1434542400000L;
    assertEquals(baseTime, BaseTimes.baseTimeFor(timestamp));
  }

  @Test(expected = NoSuchElementException.class)
  public void testBaseTimesBetweenThrowsAtEnd() throws Exception {
    final long baseTime = 1434542400000L;
    final PrimitiveIterator.OfLong baseTimes = baseTimesBetween(baseTime, baseTime);
    baseTimes.nextLong();
    baseTimes.nextLong();
  }

  @Test
  public void testBaseTimesBetweenInstantBaseTime() throws Exception {
    final long baseTime = 1434542400000L;
    final PrimitiveIterator.OfLong baseTimes = baseTimesBetween(baseTime, baseTime);
    assertEquals(baseTime, baseTimes.nextLong());
    assertFalse(baseTimes.hasNext());
  }

  @Test
  public void testBaseTimesBetweenAlignedBaseTime() throws Exception {
    final long baseTime = 1434542400000L;
    final PrimitiveIterator.OfLong baseTimes = baseTimesBetween(baseTime,
        baseTime + 2 * BaseTimes.BASE_TIME_PERIOD);

    assertEquals(baseTime, baseTimes.nextLong());
    assertEquals(baseTime + 1 * BaseTimes.BASE_TIME_PERIOD, baseTimes.nextLong());
    assertEquals(baseTime + 2 * BaseTimes.BASE_TIME_PERIOD, baseTimes.nextLong());
    assertFalse(baseTimes.hasNext());
  }

  @Test
  public void testBaseTimesBetweenSmallDuration() throws Exception {
    final long baseTime = 1434542400000L;
    final PrimitiveIterator.OfLong baseTimes = baseTimesBetween(baseTime,
        baseTime + BaseTimes.BASE_TIME_PERIOD + 1);

    assertEquals(baseTime, baseTimes.nextLong());
    assertEquals(baseTime + BaseTimes.BASE_TIME_PERIOD, baseTimes.nextLong());
    assertFalse(baseTimes.hasNext());
  }

  @Test
  public void testBaseTimesBetweenDurationOffset() throws Exception {
    final long startTime = 1434542400500L;
    final long baseTime = 1434542400000L;
    final PrimitiveIterator.OfLong baseTimes = baseTimesBetween(startTime,
        startTime + BaseTimes.BASE_TIME_PERIOD + 1);

    assertEquals(baseTime, baseTimes.nextLong());
    assertEquals(baseTime + BaseTimes.BASE_TIME_PERIOD, baseTimes.nextLong());
    assertFalse(baseTimes.hasNext());
  }
}
