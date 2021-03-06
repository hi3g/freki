package se.tre.freki.storage.cassandra.query;

import se.tre.freki.query.DataPoint;

import com.datastax.driver.core.Row;

abstract class RowDataPoint implements DataPoint {
  private Row row;

  public void setRow(final Row row) {
    this.row = row;
  }

  public Row row() {
    return row;
  }

  @Override
  public long timestamp() {
    return row().getLong("timestamp");
  }

  static class RowLongDataPoint extends RowDataPoint implements LongDataPoint {
    @Override
    public long value() {
      return row().getLong("long_value");
    }
  }

  static class RowFloatDataPoint extends RowDataPoint implements FloatDataPoint {
    @Override
    public float value() {
      return row().getFloat("float_value");
    }
  }

  static class RowDoubleDataPoint extends RowDataPoint implements DoubleDataPoint {
    @Override
    public double value() {
      return row().getDouble("double_value");
    }
  }
}
