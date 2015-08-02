package se.tre.freki.storage.cassandra.query;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import se.tre.freki.query.DataPoint;
import se.tre.freki.utils.AsyncIterator;
import se.tre.freki.utils.CollectionBackedAsyncIterator;

import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.NoSuchElementException;

public class DataPointIteratorTest {
  private Row mockRowWithColumnType(final String columnType) {
    final Row row = mock(Row.class);

    final ImmutableList<String> valueColumns = ImmutableList.of(
        "long_value", "float_value", "double_value");

    for (final String valueColumn : valueColumns) {
      if (valueColumn.equals(columnType)) {
        when(row.isNull(valueColumn)).thenReturn(false);
      } else {
        when(row.isNull(valueColumn)).thenReturn(true);
      }
    }

    return row;
  }

  private AsyncIterator<Row> rowIterator(Row... rows) {
    return new CollectionBackedAsyncIterator<>(ImmutableList.copyOf(rows));
  }

  @Test
  public void testDataPointHasRowSet() throws Exception {
    final Row row1 = mockRowWithColumnType("long_value");
    final Row row2 = mockRowWithColumnType("long_value");
    final AsyncIterator<Row> rows = rowIterator(row1, row2);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows);
    assertSame(row1, ((RowDataPoint) dataPoints.next()).row());
    assertSame(row2, ((RowDataPoint) dataPoints.next()).row());
  }

  @Test(expected = NoSuchElementException.class)
  public void testThrowsOnExhaustedIterator() throws Exception {
    final AsyncIterator<Row> rows = rowIterator();
    DataPointIterator.iteratorFor(rows).next();
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsOnNoKnownColumn() throws Exception {
    final Row row = mockRowWithColumnType("unknownColumn");
    final AsyncIterator<Row> rows = rowIterator(row);
    DataPointIterator.iteratorFor(rows).next();
  }

  @Test
  public void testYieldsDoubleDataPoint() throws Exception {
    final Row row = mockRowWithColumnType("double_value");
    final AsyncIterator<Row> rows = rowIterator(row);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows);
    assertTrue(dataPoints.next() instanceof DataPoint.DoubleDataPoint);
  }

  @Test
  public void testYieldsFloatDataPoint() throws Exception {
    final Row row = mockRowWithColumnType("float_value");
    final AsyncIterator<Row> rows = rowIterator(row);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows);
    assertTrue(dataPoints.next() instanceof DataPoint.FloatDataPoint);
  }

  @Test
  public void testYieldsLongDataPoint() throws Exception {
    final Row row = mockRowWithColumnType("long_value");
    final AsyncIterator<Row> rows = rowIterator(row);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows);
    assertTrue(dataPoints.next() instanceof DataPoint.LongDataPoint);
  }

  @Test
  public void testYieldsSameDataPoint() throws Exception {
    final Row row1 = mockRowWithColumnType("long_value");
    final Row row2 = mockRowWithColumnType("long_value");
    final AsyncIterator<Row> rows = rowIterator(row1, row2);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows);
    final DataPoint firstDataPoints = dataPoints.next();
    assertSame(firstDataPoints, dataPoints.next());
  }

}
