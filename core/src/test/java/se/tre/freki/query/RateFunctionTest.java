package se.tre.freki.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static se.tre.freki.query.DataPoint.DoubleDataPoint;

import se.tre.freki.utils.CollectionBackedAsyncIterator;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class RateFunctionTest {
  @Test
  public void testCalculatesRate() throws Exception {
    final RateFunction rateFunction = new RateFunction(
        new CollectionBackedAsyncIterator<>(ImmutableList.of(dataPoint(0, 0), dataPoint(2, 1))));

    assertEquals(2, rateFunction.next().doubleValue(), 0.00001d);
    assertFalse(rateFunction.hasNext());
  }

  @Test
  public void testHasNoDataPointOnSingle() throws Exception {
    final RateFunction rateFunction = new RateFunction(
        new CollectionBackedAsyncIterator<>(ImmutableList.of(dataPoint(0, 0))));
    assertFalse(rateFunction.hasNext());
  }

  @Test
  public void testHasNoDataPointOnNone() throws Exception {
    final RateFunction rateFunction = new RateFunction(
        new CollectionBackedAsyncIterator<>(ImmutableList.of()));
    assertFalse(rateFunction.hasNext());
  }

  @Test
  public void testCalculateComplexRate() throws Exception {
    final RateFunction rateFunction = new RateFunction(
        new CollectionBackedAsyncIterator<>(generatePeak()));

    assertEquals(1, rateFunction.next().doubleValue(), 0.00001d);
    assertEquals(1, rateFunction.next().doubleValue(), 0.00001d);
    assertEquals(1, rateFunction.next().doubleValue(), 0.00001d);
    assertEquals(-1, rateFunction.next().doubleValue(), 0.00001d);
    assertEquals(-1, rateFunction.next().doubleValue(), 0.00001d);
    assertFalse(rateFunction.hasNext());
  }

  private ImmutableList<DoubleDataPoint> generatePeak() {
    final ImmutableList.Builder<DoubleDataPoint> dataPoints = ImmutableList.builder();

    for (int i = 0; i < 6; i++) {
      double value = i <= 3 ? i : 6 - i;
      dataPoints.add(dataPoint(value, i));
    }
    return dataPoints.build();
  }


  private DoubleDataPoint dataPoint(final double value, final long timestamp) {
    return new TestDoubleDataPoint(value, timestamp);
  }

  private class TestDoubleDataPoint implements DoubleDataPoint {

    private double value;
    private long timestamp;

    public TestDoubleDataPoint(final double value, final long timestamp) {
      this.value = value;
      this.timestamp = timestamp;
    }

    @Override
    public long timestamp() {
      return timestamp;
    }

    @Override
    public double doubleValue() {
      return value;
    }
  }
}
