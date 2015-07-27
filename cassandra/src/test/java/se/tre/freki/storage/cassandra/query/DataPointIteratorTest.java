package se.tre.freki.storage.cassandra.query;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import se.tre.freki.query.DataPoint;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.NoSuchElementException;

public class DataPointIteratorTest {
  private Row mockRowWithColumnType(final String columnType) {
    final Row row = mock(Row.class);
    final ColumnDefinitions columns = mock(ColumnDefinitions.class);
    when(row.getColumnDefinitions()).thenReturn(columns);
    when(columns.contains(columnType)).thenReturn(true);
    return row;
  }

  @Test
  public void testDataPointHasRowSet() throws Exception {
    final Row row1 = mockRowWithColumnType("long_value");
    final Row row2 = mockRowWithColumnType("long_value");
    final ImmutableList<Row> rows = ImmutableList.of(row1, row2);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows.iterator());
    assertSame(row1, ((RowDataPoint) dataPoints.next()).row());
    assertSame(row2, ((RowDataPoint) dataPoints.next()).row());
  }

  @Test(expected = NoSuchElementException.class)
  public void testThrowsOnExhaustedIterator() throws Exception {
    final ImmutableList<Row> rows = ImmutableList.of();
    DataPointIterator.iteratorFor(rows.iterator()).next();
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsOnNoKnownColumn() throws Exception {
    final Row row = mockRowWithColumnType("unknownColumn");
    final ImmutableList<Row> rows = ImmutableList.of(row);
    DataPointIterator.iteratorFor(rows.iterator()).next();
  }

  @Test
  public void testYieldsDoubleDataPoint() throws Exception {
    final Row row = mockRowWithColumnType("double_value");
    final ImmutableList<Row> rows = ImmutableList.of(row);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows.iterator());
    assertTrue(dataPoints.next() instanceof DataPoint.DoubleDataPoint);
  }

  @Test
  public void testYieldsFloatDataPoint() throws Exception {
    final Row row = mockRowWithColumnType("float_value");
    final ImmutableList<Row> rows = ImmutableList.of(row);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows.iterator());
    assertTrue(dataPoints.next() instanceof DataPoint.FloatDataPoint);
  }

  @Test
  public void testYieldsLongDataPoint() throws Exception {
    final Row row = mockRowWithColumnType("long_value");
    final ImmutableList<Row> rows = ImmutableList.of(row);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows.iterator());
    assertTrue(dataPoints.next() instanceof DataPoint.LongDataPoint);
  }

  @Test
  public void testYieldsSameDataPoint() throws Exception {
    final Row row1 = mockRowWithColumnType("long_value");
    final Row row2 = mockRowWithColumnType("long_value");
    final ImmutableList<Row> rows = ImmutableList.of(row1, row2);

    final DataPointIterator dataPoints = DataPointIterator.iteratorFor(rows.iterator());
    final DataPoint firstDataPoints = dataPoints.next();
    assertSame(firstDataPoints, dataPoints.next());
  }
}
