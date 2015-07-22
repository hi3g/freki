package se.tre.freki.query.predicate;

import se.tre.freki.labels.LabelId;

import com.google.common.collect.ImmutableSet;

public class AlternationTimeSeriesIdPredicate extends TimeSeriesIdPredicate {
  private final ImmutableSet<LabelId> ids;

  public AlternationTimeSeriesIdPredicate(final ImmutableSet<LabelId> ids) {
    this.ids = ids;
  }
}
