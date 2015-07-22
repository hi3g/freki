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
}
