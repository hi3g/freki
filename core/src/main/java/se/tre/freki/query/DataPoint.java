package se.tre.freki.query;

public interface DataPoint {
  long timestamp();

  interface LongDataPoint extends DataPoint {
    long value();
  }

  interface FloatDataPoint extends DataPoint {
    float value();
  }

  interface DoubleDataPoint extends DataPoint {
    double value();
  }
}
