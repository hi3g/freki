package se.tre.freki.stats;

import static com.google.common.base.Preconditions.checkNotNull;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A {@link MetricRegistryListener} that listens for metrics that are registrered on the the
 * provided {@link com.codahale.metrics.MetricRegistry} and makes sure that the metric names have a
 * label associated with them.
 */
public class FrekiMetricRegistrator implements MetricRegistryListener {
  private static final Logger LOG = LoggerFactory.getLogger(FrekiMetricRegistrator.class);

  private final LabelClient labelClient;

  /**
   * Create a new instance that will use the provided {@link LabelClient} to create new labels. The
   * provided map of default tags will also be created when necessary.
   *
   * @param labelClient The label client to use for looking up labels
   * @param defaultTags A map of default tags to create if necessary
   */
  public FrekiMetricRegistrator(final LabelClient labelClient,
                                final Map<String, String> defaultTags) {
    this.labelClient = checkNotNull(labelClient);
    registerTags(defaultTags);
  }

  /**
   * Make sure that the label with the provided name and type exists. Will use the {@link
   * LabelClient}s label strategies for each type to resolve the names.
   *
   * @param name The name to lookup
   * @param type The type of name to lookup
   */
  private void ensureLabelExists(final String name, final LabelType type) {
    final ListenableFuture<LabelId> idFuture = labelClient.lookupId(name, type);

    Futures.addCallback(idFuture, new FutureCallback<LabelId>() {
      @Override
      public void onSuccess(final LabelId id) {
        // We have no use of the ID here. We only care about the ID
        // being found.
      }

      @Override
      public void onFailure(final Throwable throwable) {
        LOG.error("Unable to lookup ID with name {} and type {}", name, type, throwable);
      }
    });
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

  /**
   * If the provided name is an "internal" Freki name then it is expected to be encoded in the
   * format specified by {@link Metrics#name(String, Metrics.Tag...)} and the metric in it will be
   * extracted and returned. The tags in an "internal" Freki name will also be extracted and
   * "ensured" to be present.
   *
   * <p>If the name is not a Freki name then the name will be returned as is.
   *
   * @param name The name to parse
   * @return The Freki metric extracted or the name unaltered
   */
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
