package se.tre.freki.stats;

import static com.google.common.base.Preconditions.checkNotNull;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.IdLookupStrategy;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;
import javax.inject.Inject;

public class InternalMetricRegistrator implements MetricRegistryListener {
  private final LabelClient labelClient;
  private final IdLookupStrategy lookupStrategy;

  public InternalMetricRegistrator(final LabelClient labelClient,
                                   final IdLookupStrategy lookupStrategy,
                                   final Map<String, String> defaultTags) {
    this.labelClient = checkNotNull(labelClient);
    this.lookupStrategy = checkNotNull(lookupStrategy);
    registerTags(defaultTags);
  }

  private void ensureLabelExists(final String name, final LabelType type) {
    final ListenableFuture<LabelId> idFuture = lookupStrategy.getId(
        labelClient.contextForType(type), name);
  }

  @Override
  public void onGaugeAdded(final String name, final Gauge<?> gauge) {
  }

  @Override
  public void onGaugeRemoved(final String name) {

  }

  @Override
  public void onCounterAdded(final String name, final Counter counter) {
    if (Metrics.isFrekiName(name)) {
      final String metric = Metrics.metricIn(name);
      final Map<String, String> tags = Metrics.tagsIn(name);

      ensureLabelExists(metric, LabelType.METRIC);

      registerTags(tags);
    }
  }

  @Override
  public void onCounterRemoved(final String name) {
  }

  @Override
  public void onHistogramAdded(final String name, final Histogram histogram) {
  }

  @Override
  public void onHistogramRemoved(final String name) {
  }

  @Override
  public void onMeterAdded(final String name, final Meter meter) {
  }

  @Override
  public void onMeterRemoved(final String name) {
  }

  @Override
  public void onTimerAdded(final String name, final Timer timer) {
    if (Metrics.isFrekiName(name)) {
      final String metric = Metrics.metricIn(name);
      final Map<String, String> tags = Metrics.tagsIn(name);

      ensureLabelExists(metric + ".count", LabelType.METRIC);
      ensureLabelExists(metric + ".min", LabelType.METRIC);
      ensureLabelExists(metric + ".max", LabelType.METRIC);
      ensureLabelExists(metric + ".mean", LabelType.METRIC);
      ensureLabelExists(metric + ".stdDev", LabelType.METRIC);
      ensureLabelExists(metric + ".median", LabelType.METRIC);
      ensureLabelExists(metric + ".pct75th", LabelType.METRIC);
      ensureLabelExists(metric + ".pct95th", LabelType.METRIC);
      ensureLabelExists(metric + ".pct98th", LabelType.METRIC);
      ensureLabelExists(metric + ".pct99th", LabelType.METRIC);
      ensureLabelExists(metric + ".pct999th", LabelType.METRIC);

      registerTags(tags);
    }
  }

  private void registerTags(final Map<String, String> tags) {
    for (final Map.Entry<String, String> tag : tags.entrySet()) {
      ensureLabelExists(tag.getKey(), LabelType.TAGK);
      ensureLabelExists(tag.getValue(), LabelType.TAGV);
    }
  }

  @Override
  public void onTimerRemoved(final String name) {
  }
}
