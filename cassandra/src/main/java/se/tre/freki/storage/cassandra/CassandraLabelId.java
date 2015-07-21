package se.tre.freki.storage.cassandra;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
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

  protected static long generateId(final String name, final LabelType type) {
    // This discards half the hash but it should still work ok with murmur3.
    final long id = Hashing.murmur3_128().hashString(name, CassandraConst.CHARSET).asLong();

    return makeIdTypeSpecific(id, type);
  }

  protected static long makeIdTypeSpecific(final long id, final LabelType type) {

    long returnValue = id;
    returnValue &= ~0b1; // unset LSB bit
    returnValue &= ~0b10; // unset 2nd bit from LSB

    switch (type) {
      case METRIC:
        break;
      case TAGV:
        returnValue |= 0b1; // set LSB bit
        break;
      case TAGK:
        returnValue |= 0b10; // set 2nd bit from LSB
        break;
      default:
        throw new IllegalArgumentException("Unknown type.");
    }

    return returnValue;
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
