package se.tre.freki.query;

public interface DataPoint {
  long timestamp();

  DataPointType type();

  float floatValue();

  double doubleValue();

  long longValue();

  enum DataPointType {
    LONG, FLOAT, DOUBLE;

    public static DataPointType widest(final DataPointType first,
                                final DataPointType second) {
      return first.ordinal() > second.ordinal() ? first : second;
    }
  }

  interface LongDataPoint extends DataPoint {
    @Override
    default float floatValue() {
      return longValue();
    }

    @Override
    default double doubleValue() {
      return longValue();
    }

    @Override
    default DataPointType type() {
      return DataPointType.LONG;
    }
  }

  interface FloatDataPoint extends DataPoint {
    @Override
    default double doubleValue() {
      return floatValue();
    }

    @Override
    default long longValue() {
      throw new AssertionError();
    }

    @Override
    default DataPointType type() {
      return DataPointType.FLOAT;
    }
  }

  interface DoubleDataPoint extends DataPoint {
    @Override
    default float floatValue() {
      throw new AssertionError();
    }

    @Override
    default long longValue() {
      throw new AssertionError();
    }

    @Override
    default DataPointType type() {
      return DataPointType.DOUBLE;
    }
  }
}
