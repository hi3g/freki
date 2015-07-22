package se.tre.freki.storage.cassandra.query;

import se.tre.freki.query.LongDataPoint;

import com.datastax.driver.core.ResultSet;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class DataPointIterator<K extends RowDataPoint> implements Iterator<K> {
  private final ResultSet rows;
  private final K dataPoint;

  protected DataPointIterator(final ResultSet rows,
                              final K dataPoint) {
    this.rows = rows;
    this.dataPoint = dataPoint;
  }

  public static DataPointIterator iteratorFor(final ResultSet rows) {
    return null;
  }

  @Override
  public boolean hasNext() {
    return !rows.isExhausted();
  }

  private void checkHasNext(final String message) {
    if (!hasNext()) {
      throw new NoSuchElementException(message);
    }
  }

  @Override
  public K next() {
    checkHasNext("The search result does not contain any more data points");
    dataPoint.setRow(rows.one());
    return dataPoint;
  }

  private static class RowLongDataPoint extends RowDataPoint implements LongDataPoint {
    @Override
    public long value() {
      return row().getLong("long_value");
    }
  }
}
