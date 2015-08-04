package se.tre.freki.query.predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import se.tre.freki.labels.LabelId;
import se.tre.freki.query.QueryException;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

/**
 * The internal representation of the specification of which time series to return information about
 * in a "select" query.
 */
public class TimeSeriesQueryPredicate {
  private final LabelId metric;
  private final ImmutableSet<TimeSeriesTagPredicate> tagPredicates;

  protected TimeSeriesQueryPredicate(final LabelId metric,
                                     final ImmutableSet<TimeSeriesTagPredicate> tagPredicates) {
    this.metric = checkNotNull(metric);
    this.tagPredicates = checkNotNull(tagPredicates);
  }

  public LabelId metric() {
    return metric;
  }

  public ImmutableSet<TimeSeriesTagPredicate> tagPredicates() {
    return tagPredicates;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LabelId metric;
    private Set<TimeSeriesTagPredicate> tagPredicates = new HashSet<>();

    public void metric(final LabelId metric) {
      this.metric = checkNotNull(metric);
    }

    public void addTagPredicate(final TimeSeriesTagPredicate tagPredicate) {
      tagPredicates.add(checkNotNull(tagPredicate));
    }

    /**
     * Returns a newly instantiated {@code TimeSeriesQueryPredicate} with the arguments provided to
     * this builder.
     */
    public TimeSeriesQueryPredicate build() {
      checkState(metric != null);

      try {
        checkState(tagPredicates.size() > 0);
      } catch (IllegalStateException e) {
        throw new QueryException("No valid tag predicates");
      }

      return new TimeSeriesQueryPredicate(metric,
          ImmutableSet.copyOf(tagPredicates));
    }
  }
}
