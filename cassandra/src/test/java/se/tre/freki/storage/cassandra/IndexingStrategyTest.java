package se.tre.freki.storage.cassandra;

import static se.tre.freki.storage.cassandra.CassandraLabelId.fromLong;

import se.tre.freki.labels.LabelId;
import se.tre.freki.storage.cassandra.IndexStrategy.IndexingStrategy;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class IndexingStrategyTest {
  @Rule
  public final Timeout timeout = Timeout.millis(CassandraTestHelpers.TIMEOUT);

  private Session session;
  private IndexingStrategy indexingStrategy;

  @Before
  public void setUp() throws Exception {
    final CassandraTestComponent cassandraTestComponent = DaggerCassandraTestComponent.create();

    final Config config = cassandraTestComponent.config();

    // It is unfortunate that we can't get a cassandra store directly from dagger but this cast will
    // at least fail hard if it ever is any other store.
    final CassandraStoreDescriptor storeDescriptor =
        (CassandraStoreDescriptor) cassandraTestComponent.storeDescriptor();

    final Cluster cluster = storeDescriptor.createCluster(config);
    final String keyspace = config.getString("freki.storage.cassandra.keyspace");
    session = storeDescriptor.connectTo(cluster, keyspace);

    indexingStrategy = new IndexingStrategy(session);
  }

  @After
  public void tearDown() throws Exception {
    CassandraTestHelpers.truncate(session);
  }

  @Test(expected = NoSuchElementException.class)
  public void testIndexTimeseriesIdThrowsOnOddTags() throws Exception {
    final LabelId metric = fromLong(1L);

    final ImmutableList<LabelId> tags = ImmutableList.<LabelId>of(
        fromLong(2L));

    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric, tags);

    indexingStrategy.indexTimeseriesId(metric, tags, timeSeriesId);
  }

  @Test
  public void testIndexTimeseriesId() throws Exception {
    final LabelId metric = fromLong(1L);

    final ImmutableList<LabelId> tags = ImmutableList.<LabelId>of(
        fromLong(3L),
        fromLong(2L));

    final ByteBuffer timeSeriesId = TimeSeriesIds.timeSeriesId(metric, tags);

    indexingStrategy.indexTimeseriesId(metric, tags, timeSeriesId);
  }
}
