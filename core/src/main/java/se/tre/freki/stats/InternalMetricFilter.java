package se.tre.freki.stats;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class InternalMetricFilter implements MetricFilter {
  @Override
  public boolean matches(final String name, final Metric metric) {
    return Metrics.isFrekiName(name);
  }
}
