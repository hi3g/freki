package se.tre.freki.query;

import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;

public class TimeSeriesQuery {
  private final TimeSeriesQueryPredicate predicate;

  private final long startTime;
  private final long endTime;
}
