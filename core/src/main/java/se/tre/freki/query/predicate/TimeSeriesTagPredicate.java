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

  public TimeSeriesIdPredicate key() {
    return key;
  }

  public TimeSeriesIdPredicate value() {
    return value;
  }

  public TimeSeriesTagOperator operator() {
    return operator;
  }

  enum TimeSeriesTagOperator {
    EQUALS, NOT_EQUALS
  }

  public static TimeSeriesTagPredicate eq(final TimeSeriesIdPredicate key,
                                          final TimeSeriesIdPredicate value) {
    return new TimeSeriesTagPredicate(key, value, TimeSeriesTagOperator.EQUALS);
  }

  public static TimeSeriesTagPredicate neq(final TimeSeriesIdPredicate key,
                                           final TimeSeriesIdPredicate value) {
    return new TimeSeriesTagPredicate(key, value, TimeSeriesTagOperator.NOT_EQUALS);
  }
}
