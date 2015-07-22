package se.tre.freki.storage.cassandra;

import static se.tre.freki.storage.cassandra.CassandraLabelId.toLong;

import se.tre.freki.labels.LabelId;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A utility class for working with the Cassandra stores representation of time series IDs.
 */
final class TimeSeriesIds {
  private TimeSeriesIds() {
  }

  /**
   * The byte buffer representation of a time series ID that is easy to use with the {@link
   * com.datastax.driver.core.BoundStatement}.
   *
   * @param metric The metric of the time series ID
   * @param tags The tags of the time series ID
   * @return A byte buffer that contains the Cassandra stores representation of a time series ID
   */
  static ByteBuffer timeSeriesId(final LabelId metric, final List<LabelId> tags) {
    return ByteBuffer.wrap(timeSeriesIdBytes(metric, tags));
  }

  /**
   * Calculate the Cassandra stores time series representation of a time series ID for the provided
   * {@code metric} and {@code tags}.
   *
   * @param metric The metric of the time series ID
   * @param tags The tags of the time series ID
   * @return A byte array that contains the Cassandra stores representation of a time series ID
   */
  static byte[] timeSeriesIdBytes(final LabelId metric, final List<LabelId> tags) {
    Hasher tsidHasher = Hashing.murmur3_128().newHasher()
        .putLong(toLong(metric));

    for (final LabelId tag : tags) {
      tsidHasher.putLong(toLong(tag));
    }

    return tsidHasher.hash().asBytes();
  }
}
