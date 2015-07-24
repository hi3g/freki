package se.tre.freki.query.predicate;

/**
 * An ID predicate that represents the wildcard. This is the internal representation of the "*"
 * operator.
 */
public class WildcardTimeSeriesIdPredicate extends TimeSeriesIdPredicate {
  public static WildcardTimeSeriesIdPredicate wildcard() {
    return new WildcardTimeSeriesIdPredicate();
  }
}
