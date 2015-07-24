package se.tre.freki.query.predicate;

public class WildcardTimeSeriesIdPredicate extends TimeSeriesIdPredicate {
  public static WildcardTimeSeriesIdPredicate wildcard() {
    return new WildcardTimeSeriesIdPredicate();
  }
}
