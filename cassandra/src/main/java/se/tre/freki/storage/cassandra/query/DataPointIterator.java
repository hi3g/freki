package se.tre.freki.storage.cassandra.query;

import se.tre.freki.query.DataPoint;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.Row;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that is capable of interpreting data points stored within a {@link Row}. This
 * iterator is not thread safe and is effectively a view. The returned data point will change
 * between each call to {@link #next()}.
 *
 * <p>The type of the data point will be decided using the first row in the iterator given to the
 * constructor. The decision is based on which columns are present in the row.
 */
public class DataPointIterator implements Iterator<DataPoint> {
  private final Iterator<Row> rows;
  private TypeStrategy typeStrategy;

  protected DataPointIterator(final Iterator<Row> rows) {
    this.rows = rows;
  }

  /**
   * Create a new data point iterator that will read each row in the provided iterator and represent
   * it as a data point of the appropriate type.
   *
   * @param rows The rows to represent as data points
   * @return A newly instantiated data point iterator
   */
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
        return new RowDataPoint.RowLongDataPoint();
      } else if (columnDefinitions.contains("float_value")) {
        return new RowDataPoint.RowFloatDataPoint();
      } else if (columnDefinitions.contains("double_value")) {
        return new RowDataPoint.RowDoubleDataPoint();
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
