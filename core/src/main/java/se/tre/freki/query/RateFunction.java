package se.tre.freki.query;

import static se.tre.freki.query.DataPoint.DataPointType.widest;

import se.tre.freki.query.DataPoint.DataPointType;
import se.tre.freki.query.DataPoint.DoubleDataPoint;
import se.tre.freki.utils.AsyncIterator;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.NoSuchElementException;

public class RateFunction implements AsyncIterator<DoubleDataPoint> {
  private final AsyncIterator<? extends DataPoint> iterator;

  private long previousRawLong;
  private float previousRawFloat;
  private double previousRawDouble;
  private DataPointType previousType;
  private long previousTimestamp;

  private final RateDataPoint rateDataPoint;

  RateFunction(final AsyncIterator<? extends DataPoint> iterator) {
    this.iterator = iterator;
    this.rateDataPoint = new RateDataPoint();

    if (iterator.hasNext()) {
      updatePreviousValues(iterator.next());
    }
  }

  private void updatePreviousValues(final DataPoint previous) {
    this.previousType = previous.type();
    this.previousTimestamp = previous.timestamp();

    switch (previousType) {
      case LONG:
        previousRawLong = previous.longValue();
        break;
      case FLOAT:
        previousRawFloat = previous.floatValue();
        break;
      case DOUBLE:
        previousRawDouble = previous.doubleValue();
        break;
      default:
        throw new AssertionError();
    }
  }

  @Override
  public boolean hasMoreWithoutFetching() {
    return iterator.hasMoreWithoutFetching();
  }

  @Override
  public ListenableFuture<Boolean> fetchMore() {
    return iterator.fetchMore();
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  private void checkHasNext(final String message) {
    if (!hasNext()) {
      throw new NoSuchElementException(message);
    }
  }

  @Override
  public DoubleDataPoint next() {
    checkHasNext("No more data points to iterate over");

    final DataPoint nextDataPoint = iterator.next();
    final long timeDelta = nextDataPoint.timestamp() - previousTimestamp;

    rateDataPoint.value = calculateValue(nextDataPoint) / timeDelta;
    updatePreviousValues(nextDataPoint);

    return rateDataPoint;
  }

  private double calculateValue(final DataPoint nextDataPoint) {
    final DataPointType widestType = widest(previousType, nextDataPoint.type());

    switch (widestType) {
      case FLOAT:
        return (nextDataPoint.floatValue() - previousRawFloat);
      case DOUBLE:
        return (nextDataPoint.doubleValue() - previousRawDouble);
      case LONG:
        return (nextDataPoint.longValue() - previousRawLong);
      default:
        throw new AssertionError();
    }
  }

  private final class RateDataPoint implements DoubleDataPoint {
    double value;

    @Override
    public long timestamp() {
      return previousTimestamp;
    }

    @Override
    public double doubleValue() {
      return value;
    }
  }
}
