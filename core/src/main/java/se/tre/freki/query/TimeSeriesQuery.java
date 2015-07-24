package se.tre.freki.query;

import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;

/**
 * The Java representation of a select query.
 */
public class TimeSeriesQuery {
  private final TimeSeriesQueryPredicate predicate;

  private final long startTime;
  private final long endTime;

  /**
   * Create a new instance that will find time series matched by the {@code predicate} and data
   * points within the provided time range.
   *
   * @param predicate A specification of the time series to find data points for
   * @param startTime The lower bound to timestamp to fetch data points within
   * @param endTime The upper bound to timestamp to fetch data points within
   */
  public TimeSeriesQuery(final TimeSeriesQueryPredicate predicate,
                         final long startTime,
                         final long endTime) {
    this.predicate = predicate;
    this.startTime = startTime;
    this.endTime = endTime;
  }
}
