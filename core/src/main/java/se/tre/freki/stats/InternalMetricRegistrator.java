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
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class InternalMetricRegistrator implements MetricRegistryListener {
  private static final Logger LOG = LoggerFactory.getLogger(InternalMetricRegistrator.class);

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
    final Object value = gauge.getValue();

    if (value instanceof Integer
        || value instanceof Long
        || value instanceof Double) {
      final String metric = getMetricAndRegisterTags(name);
      ensureLabelExists(metric, LabelType.METRIC);
    } else {
      LOG.debug("A gauge was added with the name {} but it is not numeric: {}", name, gauge);
    }
  }

  @Override
  public void onGaugeRemoved(final String name) {

  }

  @Override
  public void onCounterAdded(final String name, final Counter counter) {
    final String metric = getMetricAndRegisterTags(name);
    ensureLabelExists(metric, LabelType.METRIC);
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
    final String metric = getMetricAndRegisterTags(name);

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

  private String getMetricAndRegisterTags(final String name) {
    if (Metrics.isFrekiName(name)) {
      final String metric = Metrics.metricIn(name);
      registerTags(Metrics.tagsIn(name));
      return metric;
    } else {
      return name;
    }
  }
}
