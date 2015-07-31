package se.tre.freki.stats;

import static com.google.common.base.Preconditions.checkNotNull;

import se.tre.freki.core.DataPointsClient;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A Metrics core reporter that periodically will write all metrics that belong to the provided
 * metric registry to the Freki database.
 */
public class FrekiMetricReporter extends ScheduledReporter {
  private static final Logger LOG = LoggerFactory.getLogger(FrekiMetricReporter.class);

  private final DataPointsClient client;
  private final ImmutableMap<String, String> defaultTags;

  /**
   * Create a new reporter that will write all metrics in the provided registry to the data points
   * client periodically. The provided map of default tags will be added to every data point
   * written.
   *
   * @param registry The registry to write metrics from
   * @param dataPointsClient The data points client to write metrics to
   * @param defaultTags The default tags to add to all data points
   */
  public FrekiMetricReporter(final MetricRegistry registry,
                             final DataPointsClient dataPointsClient,
                             final Map<String, String> defaultTags) {
    super(registry, "freki", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MICROSECONDS);
    this.client = checkNotNull(dataPointsClient);
    this.defaultTags = ImmutableMap.copyOf(defaultTags);
  }

  @Override
  public void report(final SortedMap<String, Gauge> gauges,
                     final SortedMap<String, Counter> counters,
                     final SortedMap<String, Histogram> histograms,
                     final SortedMap<String, Meter> meters,
                     final SortedMap<String, Timer> timers) {
    final long timestamp = System.currentTimeMillis();
    LOG.debug("Writing internal metrics at {}", timestamp);

    for (final Map.Entry<String, Gauge> gauge : gauges.entrySet()) {
      reportGauge(gauge.getKey(), gauge.getValue(), timestamp);
    }

    for (final Map.Entry<String, Counter> counter : counters.entrySet()) {
      reportCounter(counter.getKey(), counter.getValue(), timestamp);
    }

    for (final Map.Entry<String, Timer> timer : timers.entrySet()) {
      reportTimer(timer.getKey(), timer.getValue(), timestamp);
    }
  }

  private void reportGauge(final String name, final Gauge gauge, final long time) {
    final Object value = gauge.getValue();

    final String metric = Metrics.metricIn(name);
    final Map<String, String> tags = tags(name);

    if (value instanceof Integer) {
      client.addPoint(metric, time, (int) value, tags);
    } else if (value instanceof Long) {
      client.addPoint(metric, time, (long) value, tags);
    } else if (value instanceof Double) {
      client.addPoint(metric, time, (double) value, tags);
    }
  }

  private void reportCounter(final String name, final Counter counter, final long timestamp) {
    final String metric = Metrics.metricIn(name);
    final Map<String, String> tags = tags(name);

    final long value = counter.getCount();

    client.addPoint(metric, timestamp, value, tags);
  }

  private void reportTimer(final String name, final Timer timer, final long time) {
    final Snapshot snapshot = timer.getSnapshot();

    final String metric = Metrics.metricIn(name);
    final Map<String, String> tags = tags(name);

    client.addPoint(metric + ".count", time, timer.getCount(), tags);
    client.addPoint(metric + ".min", time, convertDuration(snapshot.getMin()), tags);
    client.addPoint(metric + ".max", time, convertDuration(snapshot.getMax()), tags);
    client.addPoint(metric + ".mean", time, convertDuration(snapshot.getMean()), tags);
    client.addPoint(metric + ".stdDev", time, convertDuration(snapshot.getStdDev()), tags);
    client.addPoint(metric + ".median", time, convertDuration(snapshot.getMedian()), tags);
    client.addPoint(metric + ".pct75th", time, convertDuration(snapshot.get75thPercentile()), tags);
    client.addPoint(metric + ".pct95th", time, convertDuration(snapshot.get95thPercentile()), tags);
    client.addPoint(metric + ".pct98th", time, convertDuration(snapshot.get98thPercentile()), tags);
    client.addPoint(metric + ".pct99th", time, convertDuration(snapshot.get99thPercentile()), tags);
    client.addPoint(metric + ".pct999th", time, convertDuration(snapshot.get999thPercentile()),
        tags);
  }

  private Map<String, String> tags(final String name) {
    if (Metrics.isFrekiName(name)) {
      final Map<String, String> tags = Metrics.tagsIn(name);
      tags.putAll(defaultTags);
      return tags;
    }

    return defaultTags;
  }
}
