package se.tre.freki.storage.cassandra;

import static se.tre.freki.storage.cassandra.CassandraLabelId.fromLong;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.TimeSeriesId;

import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableList;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public class CassandraTimeSeriesId extends TimeSeriesId {
  private final Row row;

  CassandraTimeSeriesId(final Row row) {
    this.row = row;
  }

  @Nonnull
  @Override
  public LabelId metric() {
    return fromLong(row.getLong("metric"));
  }

  @Nonnull
  @Override
  public List<LabelId> tags() {
    final Map<Long, Long> tags = row.getMap("tags", Long.class, Long.class);
    final ImmutableList.Builder<LabelId> labelIds = ImmutableList.builder();

    for (final Map.Entry<Long, Long> tagEntry : tags.entrySet()) {
      labelIds.add(fromLong(tagEntry.getKey()));
      labelIds.add(fromLong(tagEntry.getValue()));
    }

    return labelIds.build();
  }

  ByteBuffer timeSeriesId() {
    return row.getBytesUnsafe("timeseries_id");
  }
}
