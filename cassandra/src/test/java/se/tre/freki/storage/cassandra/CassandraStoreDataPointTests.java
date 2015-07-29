package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.tre.freki.storage.cassandra.BaseTimes.baseTimeFor;
import static se.tre.freki.storage.cassandra.CassandraConst.BASE_TIME_PERIOD;
import static se.tre.freki.storage.cassandra.CassandraLabelId.fromLong;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.StaticTimeSeriesId;
import se.tre.freki.query.DataPoint;
import se.tre.freki.query.DataPoint.LongDataPoint;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.ResultSet;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class CassandraStoreDataPointTests {
  @Rule
  public final Timeout timeout = Timeout.millis(CassandraTestHelpers.TIMEOUT);

  private CassandraStore store;

  private LabelId metric1;
  private ImmutableList<LabelId> tags1;
  private ImmutableList<LabelId> tags2;

  @Before
  public void setUp() throws Exception {
    final CassandraTestComponent cassandraTestComponent = DaggerCassandraTestComponent.create();
    final Config config = cassandraTestComponent.config();

    // It is unfortunate that we can't get a cassandra store directly from dagger but this cast will
    // at least fail hard if it ever is any other store.
    final CassandraStoreDescriptor storeDescriptor = (CassandraStoreDescriptor)
        cassandraTestComponent.storeDescriptor();

    store = storeDescriptor.createStore(config, new MetricRegistry());

    metric1 = fromLong(1L);
    metric1 = fromLong(2L);

    tags1 = ImmutableList.<LabelId>of(
        fromLong(1L),
        fromLong(2L));
    tags2 = ImmutableList.<LabelId>of(
        fromLong(1L),
        fromLong(3L));
  }

  @After
  public void tearDown() throws Exception {
    CassandraTestHelpers.truncate(store.getSession());
  }

  @Test
  public void testFetchTimeSeriesPartitionFindsExactTime() throws Exception {
    final StaticTimeSeriesId staticTimeSeriesId = new StaticTimeSeriesId(metric1, tags1);
    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric1, tags1);

    final long pointTime = 123123123;
    final long pointValue = 123123;

    store.addPoint(staticTimeSeriesId, pointTime, pointValue).get();

    final ResultSet rows = store.fetchTimeSeriesPartition(timeSeriesId, baseTimeFor(pointTime),
        pointTime, pointTime).get();

    assertEquals(pointValue, rows.one().getLong("long_value"));
    assertTrue(rows.isExhausted());
  }

  @Test
  public void testFetchTimeSeriesPartitionDoesNotFindOutsideTime() throws Exception {
    final StaticTimeSeriesId staticTimeSeriesId = new StaticTimeSeriesId(metric1, tags1);
    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric1, tags1);

    final long pointTime = 123123123;
    final long pointValue = 123123;

    store.addPoint(staticTimeSeriesId, pointTime, pointValue).get();

    final ResultSet rows = store.fetchTimeSeriesPartition(timeSeriesId, baseTimeFor(pointTime),
        pointTime + 1, pointTime + 1).get();

    assertTrue(rows.isExhausted());
  }

  @Test
  public void testFetchTimeSeriesPartitionDoesNotFindOtherTimeSeries() throws Exception {
    // Create a time series id for use with the add point call
    final StaticTimeSeriesId staticTimeSeriesId = new StaticTimeSeriesId(metric1, tags1);

    // A second different time series id that should not find the point added to the above time
    // series.
    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric1, tags2);

    final long pointTime = 123123123;
    final long pointValue = 123123;

    store.addPoint(staticTimeSeriesId, pointTime, pointValue).get();

    final ResultSet rows = store.fetchTimeSeriesPartition(timeSeriesId, baseTimeFor(pointTime),
        pointTime, pointTime).get();

    assertTrue(rows.isExhausted());
  }

  @Test
  public void testFetchTimeSeriesMultiplePartitions() throws Exception {
    final StaticTimeSeriesId staticTimeSeriesId = new StaticTimeSeriesId(metric1, tags1);
    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric1, tags1);

    final long pointTime = 123123123;
    final long firstValue = 123123;
    final long secondValue = 123124;

    store.addPoint(staticTimeSeriesId, pointTime, firstValue).get();
    store.addPoint(staticTimeSeriesId, pointTime + BASE_TIME_PERIOD, secondValue).get();

    final Iterator<? extends DataPoint> dataPoints = store.fetchTimeSeries(timeSeriesId, pointTime,
        pointTime + BASE_TIME_PERIOD);

    assertEquals(firstValue, ((LongDataPoint) dataPoints.next()).value());
    assertEquals(secondValue, ((LongDataPoint) dataPoints.next()).value());
    assertFalse(dataPoints.hasNext());
  }

  @Ignore
  @Test(expected = IllegalArgumentException.class)
  public void testFetchTimeSeriesMixedTypes() throws Exception {
    final StaticTimeSeriesId staticTimeSeriesId = new StaticTimeSeriesId(metric1, tags1);
    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric1, tags1);

    final long pointTime = 123123123;
    final long pointLongValue = 123123;
    final double pointDoubleValue = 123.32d;

    store.addPoint(staticTimeSeriesId, pointTime, pointLongValue).get();
    store.addPoint(staticTimeSeriesId, pointTime + 5, pointDoubleValue).get();

    final Iterator<? extends DataPoint> dataPoints = store.fetchTimeSeries(timeSeriesId, pointTime,
        pointTime + 5);

    final DataPoint firstDataPoint = dataPoints.next();
    assertTrue(firstDataPoint instanceof LongDataPoint);

    final LongDataPoint secondDataPoint = (LongDataPoint) dataPoints.next();
    secondDataPoint.value();
  }
}
