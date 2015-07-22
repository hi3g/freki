package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertArrayEquals;
import static se.tre.freki.storage.cassandra.CassandraLabelId.fromLong;

import se.tre.freki.labels.LabelId;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TimeSeriesIdsTest {
  private LabelId metric;
  private List<LabelId> tags;

  @Before
  public void setUp() throws Exception {
    metric = fromLong(1L);

    tags = ImmutableList.<LabelId>of(
        fromLong(1L),
        fromLong(2L));
  }

  @Test
  public void testTimeSeriesIdBytesIsIdentityFunction() throws Exception {
    assertArrayEquals(
        new byte[] {30, -121, -66, 9, -20, -71, 123, -110, 71, -5, -106, -42, -90, 64, -55, 108},
        TimeSeriesIds.timeSeriesIdBytes(metric, tags));
  }
}
