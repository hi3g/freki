package se.tre.freki.stats;

import static com.google.common.base.Preconditions.checkNotNull;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.IdLookupStrategy.CreatingIdLookupStrategy;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class InternalMetricRegistrator implements MetricRegistryListener {
  private static final Logger LOG = LoggerFactory.getLogger(InternalMetricRegistrator.class);

  private final LabelClient labelClient;
  private final CreatingIdLookupStrategy lookupStrategy;

  public InternalMetricRegistrator(final LabelClient labelClient) {
    this.labelClient = checkNotNull(labelClient);
    lookupStrategy = new CreatingIdLookupStrategy();
  }

  private boolean isFrekiName(final String name) {
    return name.startsWith("freki");
  }

  private void ensureLabelExists(final String name, final LabelType type) {
    final ListenableFuture<Optional<LabelId>> idFuture = labelClient.getLabelId(type, name);
    new CreatingIdLookupStrategy();

    Futures.addCallback(idFuture, new FutureCallback<Optional<LabelId>>() {
      @Override
      public void onSuccess(final Optional<LabelId> id) {
        if (!id.isPresent()) {
          LOG.info("Creating label for internal metric {} with type {}", name, type);
          labelClient.createId(type, name);
        }
      }

      @Override
      public void onFailure(final Throwable t) {

      }
    });
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
