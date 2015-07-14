package se.tre.freki.storage.cassandra;

import se.tre.freki.labels.LabelId;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Longs;

import javax.annotation.Nonnull;

public class CassandraLabelId implements LabelId<CassandraLabelId> {
  private final long id;

  CassandraLabelId(final long id) {
    this.id = id;
  }

  public static LabelId fromLong(final long id) {
    return new CassandraLabelId(id);
  }

  public static long toLong(final LabelId labelId) {
    return ((CassandraLabelId) labelId).id;
  }

  @Override
  public int compareTo(final CassandraLabelId that) {
    return Long.compare(id, that.id);
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(id);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CassandraLabelId other = (CassandraLabelId) obj;
    return this.id == other.id;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .toString();
  }
  
  static class CassandraLabelIdSerializer implements LabelIdSerializer<CassandraLabelId> {
    @Nonnull
    @Override
    public String serialize(final CassandraLabelId identifier) {
      return Long.toString(identifier.id);
    }
  }

  static class CassandraLabelIdDeserializer implements LabelIdDeserializer<CassandraLabelId> {
    @Nonnull
    @Override
    public CassandraLabelId deserialize(final String stringIdentifier) {
      return new CassandraLabelId(Long.parseLong(stringIdentifier));
    }
  }
}
