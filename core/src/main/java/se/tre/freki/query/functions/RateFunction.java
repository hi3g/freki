package se.tre.freki.query.functions;

import static se.tre.freki.query.DataPoint.DataPointType.widest;

import se.tre.freki.query.DataPoint;
import se.tre.freki.query.DataPoint.DataPointType;
import se.tre.freki.query.DataPoint.DoubleDataPoint;
import se.tre.freki.utils.AsyncIterator;

public class RateFunction extends QueryFunction<DoubleDataPoint> {
  private long previousRawLong;
  private float previousRawFloat;
  private double previousRawDouble;
  private DataPointType previousType;
  private long previousTimestamp;

  private final RateDataPoint rateDataPoint;

  RateFunction(final AsyncIterator<? extends DataPoint> iterator) {
    super(iterator);

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
  public DoubleDataPoint next() {
    checkHasNext("No more data points to iterate over");

    final DataPoint nextDataPoint = iterator().next();
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
