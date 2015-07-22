package se.tre.freki.storage.cassandra;

/**
 * Utility class for working with the base times as used in the Cassandra store.
 */
final class BaseTimes {
  private BaseTimes() {
  }

  /**
   * Calculate the base time based on a timestamp to be used in a partition key.
   */
  static long baseTimeFor(final long timestamp) {
    return (timestamp - (timestamp % CassandraConst.BASE_TIME_PERIOD));
  }
}
