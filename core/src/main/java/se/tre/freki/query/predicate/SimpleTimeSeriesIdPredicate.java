package se.tre.freki.query.predicate;

import se.tre.freki.labels.LabelId;

public class SimpleTimeSeriesIdPredicate extends TimeSeriesIdPredicate {
  private final LabelId id;

  public SimpleTimeSeriesIdPredicate(final LabelId id) {
    this.id = id;
  }

  public static SimpleTimeSeriesIdPredicate id(final LabelId id) {
    return new SimpleTimeSeriesIdPredicate(id);
  }
}
