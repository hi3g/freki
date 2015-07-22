package se.tre.freki.query.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import se.tre.freki.labels.LabelId;

import com.google.common.collect.ImmutableSet;

public class TimeSeriesQueryPredicate {
  private final LabelId metric;
  private final ImmutableSet<TimeSeriesTagPredicate> tagPredicates;

  protected TimeSeriesQueryPredicate(final LabelId metric,
                                     final ImmutableSet<TimeSeriesTagPredicate> tagPredicates) {
    this.metric = checkNotNull(metric);

    checkArgument(!tagPredicates.isEmpty());
    this.tagPredicates = tagPredicates;
  }
}
