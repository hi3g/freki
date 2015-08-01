package se.tre.freki.storage.cassandra;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * Utility class for working with the base times as used in the Cassandra store.
 */
final class BaseTimes {
  /** Max time delta (in milliseconds) we store in a column qualifier. */
  public static final int BASE_TIME_PERIOD = 3600000;

  private BaseTimes() {
  }

  /**
   * Calculate the base time based on a timestamp to be used in a partition key.
   */
  static long baseTimeFor(final long timestamp) {
    return (timestamp - (timestamp % BASE_TIME_PERIOD));
  }

  /**
   * Build an iterator that will yield all base times (inclusive) between the provided start and end
   * timestamp.
   *
   * @param start The timestamp the iterator will start yielding base times from
   * @param end The timestamp the iterator will stop yielding base times at
   * @return A primitive iterator that yields base times within the provided timestamps
   * @see se.tre.freki.storage.cassandra.BaseTimes.BaseTimeGenerator
   */
  static PrimitiveIterator.OfLong baseTimesBetween(final long start, final long end) {
    return new BaseTimeGenerator(start, end);
  }

  private static class BaseTimeGenerator implements PrimitiveIterator.OfLong {
    private final long end;

    private long current;

    public BaseTimeGenerator(final long start, final long end) {
      this.end = end;
      this.current = start;
    }

    @Override
    public boolean hasNext() {
      return current < end + BASE_TIME_PERIOD - 1;
    }

    @Override
    public long nextLong() {
      if (!hasNext()) {
        throw new NoSuchElementException("End of time range has been reached");
      }

      final long baseTime = baseTimeFor(current);
      current += BASE_TIME_PERIOD;
      return baseTime;
    }
  }
}
