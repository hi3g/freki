package se.tre.freki.query;

import se.tre.freki.utils.AsyncIterator;

import com.google.common.base.MoreObjects;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ResultEntry implements
    Map.Entry<DecoratedTimeSeriesId, AsyncIterator<? extends DataPoint>> {
  private final DecoratedTimeSeriesId key;
  private final AsyncIterator<? extends DataPoint> value;

  public ResultEntry(final DecoratedTimeSeriesId key,
                     final AsyncIterator<? extends DataPoint> value) {
    this.key = key;
    this.value = value;
  }

  @Nonnull
  @Override
  public DecoratedTimeSeriesId getKey() {
    return key;
  }

  @Nonnull
  @Override
  public AsyncIterator<? extends DataPoint> getValue() {
    return value;
  }

  @Override
  public AsyncIterator<? extends DataPoint> setValue(
      final AsyncIterator<? extends DataPoint> newValue) {
    throw new UnsupportedOperationException(ResultEntry.class + " instances are immutable");
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (this == object) {
      return true;
    }

    if (object instanceof ResultEntry) {
      ResultEntry that = (ResultEntry) object;
      return Objects.equals(this.getKey(), that.getKey())
             && Objects.equals(this.getValue(), that.getValue());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getKey(), getValue());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("key", getKey())
        .add("value", getValue())
        .toString();
  }
}
