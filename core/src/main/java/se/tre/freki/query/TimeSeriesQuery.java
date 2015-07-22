package se.tre.freki.query;

import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;

public class TimeSeriesQuery {
  private final TimeSeriesQueryPredicate predicate;

  private final long startTime;
  private final long endTime;

  public TimeSeriesQuery(final TimeSeriesQueryPredicate predicate,
                         final long startTime,
                         final long endTime) {
    this.predicate = predicate;
    this.startTime = startTime;
    this.endTime = endTime;
  }
}
