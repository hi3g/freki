package se.tre.freki.storage.cassandra.query;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import se.tre.freki.storage.cassandra.query.RowDataPoint.RowDoubleDataPoint;
import se.tre.freki.storage.cassandra.query.RowDataPoint.RowFloatDataPoint;
import se.tre.freki.storage.cassandra.query.RowDataPoint.RowLongDataPoint;

import com.datastax.driver.core.Row;
import org.junit.Test;

public class RowDataPointTest {
  private static final long TIMESTAMP = 123;
  private static final long LONG_VALUE = 124;
  private static final float FLOAT_VALUE = 124.5f;
  private static final double DOUBLE_VALUE = 124.023d;
  private static final double ACCEPTED_DELTA = 0.00001d;

  @Test
  public void testTimestamp() throws Exception {
    final Row row = mock(Row.class);
    when(row.getLong(eq("timestamp"))).thenReturn(TIMESTAMP);
    final RowLongDataPoint dataPoint = new RowLongDataPoint();
    dataPoint.setRow(row);
    assertEquals(TIMESTAMP, dataPoint.timestamp());
  }

  @Test
  public void testValueLong() throws Exception {
    final Row row = mock(Row.class);
    when(row.getLong(eq("long_value"))).thenReturn(LONG_VALUE);
    final RowLongDataPoint dataPoint = new RowLongDataPoint();
    dataPoint.setRow(row);
    assertEquals(LONG_VALUE, dataPoint.longValue());
  }

  @Test
  public void testValueFloat() throws Exception {
    final Row row = mock(Row.class);
    when(row.getFloat(eq("float_value"))).thenReturn(FLOAT_VALUE);
    final RowFloatDataPoint dataPoint = new RowFloatDataPoint();
    dataPoint.setRow(row);
    assertEquals(FLOAT_VALUE, dataPoint.floatValue(), ACCEPTED_DELTA);
  }

  @Test
  public void testValueDouble() throws Exception {
    final Row row = mock(Row.class);
    when(row.getDouble(eq("double_value"))).thenReturn(DOUBLE_VALUE);
    final RowDoubleDataPoint dataPoint = new RowDoubleDataPoint();
    dataPoint.setRow(row);
    assertEquals(DOUBLE_VALUE, dataPoint.doubleValue(), ACCEPTED_DELTA);
  }
}
