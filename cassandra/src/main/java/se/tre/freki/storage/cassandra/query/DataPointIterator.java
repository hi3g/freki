package se.tre.freki.storage.cassandra.query;

import se.tre.freki.query.DataPoint;
import se.tre.freki.query.LongDataPoint;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.Row;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class DataPointIterator implements Iterator<DataPoint> {
  private final Iterator<Row> rows;
  private TypeStrategy typeStrategy;

  protected DataPointIterator(final Iterator<Row> rows) {
    this.rows = rows;
  }

  public static DataPointIterator iteratorFor(final Iterator<Row> rows) {
    final DataPointIterator dataPointIterator = new DataPointIterator(rows);
    dataPointIterator.typeStrategy = new DetectingTypeStrategy(dataPointIterator);
    return dataPointIterator;
  }

  private void checkHasNext(final String message) {
    if (!hasNext()) {
      throw new NoSuchElementException(message);
    }
  }

  @Override
  public boolean hasNext() {
    return rows.hasNext();
  }

  @Override
  public DataPoint next() {
    checkHasNext("The search result does not contain any more data points");
    return typeStrategy.dataPoint(rows.next());
  }

  private static class RowLongDataPoint extends RowDataPoint implements LongDataPoint {
    @Override
    public long value() {
      return row().getLong("long_value");
    }
  }

  private interface TypeStrategy {
    RowDataPoint dataPoint(final Row row);
  }

  private static class DetectingTypeStrategy implements TypeStrategy {
    private final DataPointIterator dataPointIterator;

    public DetectingTypeStrategy(final DataPointIterator dataPointIterator) {
      this.dataPointIterator = dataPointIterator;
    }

    @Override
    public RowDataPoint dataPoint(final Row row) {
      final RowDataPoint dataPoint = dataPointFor(row);
      dataPoint.setRow(row);
      dataPointIterator.typeStrategy = new StaticTypeStrategy(dataPoint);
      return dataPoint;
    }

    private RowDataPoint dataPointFor(final Row row) {
      final ColumnDefinitions columnDefinitions = row.getColumnDefinitions();

      if (columnDefinitions.contains("long_value")) {
        return new RowLongDataPoint();
      } else {
        throw new AssertionError("row does not contain any known type");
      }
    }
  }

  private static class StaticTypeStrategy implements TypeStrategy {
    private final RowDataPoint dataPoint;

    public StaticTypeStrategy(final RowDataPoint dataPoint) {
      this.dataPoint = dataPoint;
    }

    @Override
    public RowDataPoint dataPoint(final Row row) {
      dataPoint.setRow(row);
      return dataPoint;
    }
  }
}
