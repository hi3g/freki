package se.tre.freki.query.predicate;

public class TimeSeriesTagPredicate {
  private final TimeSeriesIdPredicate key;
  private final TimeSeriesIdPredicate value;
  private final TimeSeriesTagOperator operator;

  protected TimeSeriesTagPredicate(final TimeSeriesIdPredicate key,
                                   final TimeSeriesIdPredicate value,
                                   final TimeSeriesTagOperator operator) {
    this.key = key;
    this.value = value;
    this.operator = operator;
  }

  enum TimeSeriesTagOperator {
    EQUALS, NOT_EQUALS
  }
}
