package se.tre.freki.query.predicate;

import se.tre.freki.labels.LabelId;

import com.google.common.collect.ImmutableSet;

public class TimeSeriesQueryPredicate {
  private final LabelId metric;
  private final ImmutableSet<TimeSeriesTagPredicate> tagPredicates;
}
