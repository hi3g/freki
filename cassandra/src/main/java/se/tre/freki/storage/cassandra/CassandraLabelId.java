package se.tre.freki.storage.cassandra;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

import javax.annotation.Nonnull;

public class CassandraLabelId implements LabelId<CassandraLabelId> {
  private static final long METRIC_MASK = 0xFFFFFFFFFFFFFFFCL;
  private static final long TAGK_MASK   = 0xFFFFFFFFFFFFFFFEL;
  private static final long TAGV_MASK   = 0xFFFFFFFFFFFFFFFDL;

  private final long id;

  CassandraLabelId(final long id) {
    this.id = id;
  }

  public static CassandraLabelId fromLong(final long id) {
    return new CassandraLabelId(id);
  }

  /**
   * Generate a "random" long ID for the provided name. This is currently implemented using murmur3
   * but this may change in the future. The generated ID will be masked so its type can later be
   * identified by just looking at the ID. Note that this mask removes 2-bits from the ID space
   * which means that there can only be 2^62-1 IDs.
   *
   * @param name The name of the new label to generate an ID for
   * @param type The type of the new ID
   * @return A generated masked ID
   */
  protected static long generateId(final String name, final LabelType type) {
    // This discards half the hash but it should still work ok with murmur3.
    final long id = Hashing.murmur3_128().hashString(name, CassandraConst.CHARSET).asLong();

    return makeIdTypeSpecific(id, type);
  }

  /**
   * Mask the provided ID to be of the given type.
   *
   * @param id The ID to mask
   * @param type The type of ID to mask it to
   * @return The masked ID
   */
  protected static long makeIdTypeSpecific(final long id, final LabelType type) {
    switch (type) {
      case METRIC:
        return id & METRIC_MASK;
      case TAGV:
        return id & TAGV_MASK;
      case TAGK:
        return id & TAGK_MASK;
      default:
        throw new AssertionError("The switch should have covered all cases but did not. Type is "
                                 + type);
    }
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

  /**
   * Get the type of label this ID represents.
   */
  public LabelType type() {
    if ((id | METRIC_MASK) == METRIC_MASK) {
      return LabelType.METRIC;
    } else if ((id | TAGK_MASK) == TAGK_MASK) {
      return LabelType.TAGK;
    } else if ((id | TAGV_MASK) == TAGV_MASK) {
      return LabelType.TAGV;
    } else {
      throw new AssertionError("The type of the ID " + id + " could not be determined");
    }
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
