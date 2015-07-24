package se.tre.freki.query.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import se.tre.freki.labels.LabelId;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class TimeSeriesQueryPredicate {
  private final LabelId metric;
  private final ImmutableSet<TimeSeriesTagPredicate> tagPredicates;

  protected TimeSeriesQueryPredicate(final LabelId metric,
                                     final ImmutableSet<TimeSeriesTagPredicate> tagPredicates) {
    this.metric = checkNotNull(metric);

    checkArgument(!tagPredicates.isEmpty());
    this.tagPredicates = tagPredicates;
  }

  public static Builder builder() {
    return new Builder();
  }

  private static class Builder {
    private LabelId metric;
    private Set<TimeSeriesTagPredicate> tagPredicates = new HashSet<>();

    public void metric(final LabelId metric) {
      this.metric = checkNotNull(metric);
    }

    public void addTagPredicate(final TimeSeriesTagPredicate tagPredicate) {
      tagPredicates.add(tagPredicate);
    }

    public TimeSeriesQueryPredicate build() {
      checkState(metric != null);
      checkState(!tagPredicates.isEmpty());

      return new TimeSeriesQueryPredicate(metric,
          ImmutableSet.copyOf(tagPredicates));
    }
  }
}
