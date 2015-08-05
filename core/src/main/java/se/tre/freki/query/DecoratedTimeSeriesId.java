package se.tre.freki.query;

import com.google.auto.value.AutoValue;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * The string name representation of the IDs in a {@link se.tre.freki.labels.TimeSeriesId time
 * series ID}.
 */
@AutoValue
public abstract class DecoratedTimeSeriesId {
  /**
   * Create an instance with the provided information.
   */
  public static DecoratedTimeSeriesId create(final String metric,
                                             final List<String> tags) {
    return new AutoValue_DecoratedTimeSeriesId(metric, tags);
  }

  /**
   * The metric name of the time series.
   */
  @Nonnull
  public abstract String metric();

  /**
   * The tag names behind the time series.
   */
  @Nonnull
  public abstract List<String> tags();
}
