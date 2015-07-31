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

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class InternalReporter extends ScheduledReporter {
  private final DataPointsClient client;
  private final ImmutableMap<String, String> defaultTags;

  public InternalReporter(final MetricRegistry registry,
                          final MetricFilter filter,
                          final DataPointsClient dataPointsClient,
                          final Map<String, String> defaultTags) {
    super(registry, "internal", filter, TimeUnit.SECONDS, TimeUnit.MICROSECONDS);
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

    for (final Map.Entry<String, Counter> counter : counters.entrySet()) {
      reportCounter(counter.getKey(), counter.getValue(), timestamp);
    }

    for (final Map.Entry<String, Timer> timer : timers.entrySet()) {
      reportTimer(timer.getKey(), timer.getValue(), timestamp);
    }
  }

  private void reportCounter(final String name, final Counter counter, final long timestamp) {
    final String metric = Metrics.metricIn(name);
    final Map<String, String> tags = Metrics.tagsIn(name);
    tags.putAll(defaultTags);

    final long value = counter.getCount();

    client.addPoint(metric, timestamp, value, tags);
  }

  private void reportTimer(final String name, final Timer timer, final long time) {
    final Snapshot snapshot = timer.getSnapshot();

    final String metric = Metrics.metricIn(name);
    final Map<String, String> tags = Metrics.tagsIn(name);
    tags.putAll(defaultTags);

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
    client.addPoint(metric + ".pct999th", time, convertDuration(snapshot.get999thPercentile()), tags);
  }
}
