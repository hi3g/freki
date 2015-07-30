package se.tre.freki.query.predicate;

import se.tre.freki.labels.LabelId;

import com.google.common.collect.ImmutableSet;

import java.util.List;

/**
 * An ID predicate that represents several different IDs. This is the internal representation of the
 * "or" operator "|".
 */
public class AlternationTimeSeriesIdPredicate extends TimeSeriesIdPredicate {
  private final ImmutableSet<LabelId> ids;

  public AlternationTimeSeriesIdPredicate(final ImmutableSet<LabelId> ids) {
    this.ids = ids;
  }

  public static AlternationTimeSeriesIdPredicate ids(LabelId... ids) {
    final ImmutableSet<LabelId> immutableIds = ImmutableSet.copyOf(ids);
    return new AlternationTimeSeriesIdPredicate(immutableIds);
  }

  public static AlternationTimeSeriesIdPredicate ids(List<LabelId> ids) {
    final ImmutableSet<LabelId> immutableIds = ImmutableSet.copyOf(ids);
    return new AlternationTimeSeriesIdPredicate(immutableIds);
  }

  public ImmutableSet<LabelId> ids() {
    return ids;
  }
}
