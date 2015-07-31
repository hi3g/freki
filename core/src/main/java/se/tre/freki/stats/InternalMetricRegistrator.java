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

import java.util.Map;
import javax.inject.Inject;

public class InternalMetricRegistrator implements MetricRegistryListener {
  private final LabelClient labelClient;
  private final IdLookupStrategy lookupStrategy;

  @Inject
  public InternalMetricRegistrator(final LabelClient labelClient,
                                   final IdLookupStrategy lookupStrategy) {
    this.labelClient = checkNotNull(labelClient);
    this.lookupStrategy = checkNotNull(lookupStrategy);
  }

  private boolean isFrekiName(final String name) {
    return name.startsWith("freki");
  }

  private void ensureLabelExists(final String name, final LabelType type) {
    final ListenableFuture<LabelId> idFuture = lookupStrategy.getId(
        labelClient.contextForType(type), name);
  }

  private void registerName(final String name) {
    if (isFrekiName(name)) {
      final String metric = Metrics.metricIn(name);
      final Map<String, String> tags = Metrics.tagsIn(name);

      ensureLabelExists(metric, LabelType.METRIC);

      for (final Map.Entry<String, String> tag : tags.entrySet()) {
        ensureLabelExists(tag.getKey(), LabelType.TAGK);
        ensureLabelExists(tag.getValue(), LabelType.TAGV);
      }
    }
  }

  @Override
  public void onGaugeAdded(final String name, final Gauge<?> gauge) {
    registerName(name);
  }

  @Override
  public void onGaugeRemoved(final String name) {

  }

  @Override
  public void onCounterAdded(final String name, final Counter counter) {
    registerName(name);
  }

  @Override
  public void onCounterRemoved(final String name) {

  }

  @Override
  public void onHistogramAdded(final String name, final Histogram histogram) {
    registerName(name);
  }

  @Override
  public void onHistogramRemoved(final String name) {

  }

  @Override
  public void onMeterAdded(final String name, final Meter meter) {
    registerName(name);
  }

  @Override
  public void onMeterRemoved(final String name) {

  }

  @Override
  public void onTimerAdded(final String name, final Timer timer) {
    registerName(name);
  }

  @Override
  public void onTimerRemoved(final String name) {

  }
}
