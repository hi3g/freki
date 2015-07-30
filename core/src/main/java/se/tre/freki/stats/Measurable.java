package se.tre.freki.stats;

import com.codahale.metrics.MetricRegistry;

/**
 * An interface that marks that implementors are able to register metrics with a {@link
 * MetricRegistry}.
 */
public interface Measurable {
  /**
   * Register the metrics the implementation has with the provided metric registry.
   *
   * @param registry The metric registry to register the metrics with
   */
  void registerMetricsWith(final MetricRegistry registry);
}
