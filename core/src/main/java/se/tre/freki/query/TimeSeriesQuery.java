package se.tre.freki.query;

import static com.google.common.base.Preconditions.checkState;

import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;

import com.google.auto.value.AutoValue;

/**
 * The Java representation of a select query.
 */
@AutoValue
public abstract class TimeSeriesQuery {
  public static Builder builder() {
    return new AutoValue_TimeSeriesQuery.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    abstract TimeSeriesQuery autoBuild();

    /**
     * Build an instance with the previously set information.
     */
    public TimeSeriesQuery build() {
      final TimeSeriesQuery query = autoBuild();
      checkState(query.startTime() >= 0);
      checkState(query.endTime() >= 0);
      checkState(query.endTime() >= query.startTime());
      return query;
    }

    public abstract Builder startTime(final long startTime);

    public abstract Builder endTime(final long endTime);

    public abstract Builder predicate(final TimeSeriesQueryPredicate predicate);
  }

  /**
   * Hide the constructor and prevent subclasses other than the one provided by {@link AutoValue}.
   */
  TimeSeriesQuery() {
  }

  public abstract TimeSeriesQueryPredicate predicate();

  public abstract long startTime();

  public abstract long endTime();
}
