package se.tre.freki.plugins;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableMap;

import java.io.Closeable;
import java.util.Map;

/**
 * Base plugin interface for all plugin types used by Freki.
 */
public abstract class Plugin implements Closeable {
  /**
   * The version of the plugin that implements this abstract class. All plugins are expected to
   * follow the <a href="http://semver.org/">Semantic Versioning 2</a> specification.
   *
   * @return The string representation of the version of the plugin that implements this interface
   * @see <a href="http://semver.org/">Semantic Versioning 2</a>
   */
  public abstract String version();

  /**
   * May be called to retrieve references to the metrics that this plugin exposes. There are no
   * guarantees that this will ever be called.
   *
   * @return A map of {@link Metric}s mapped to their names as defined by metrics-core
   */
  public MetricSet metrics() {
    return new MetricSet() {
      @Override
      public Map<String, Metric> getMetrics() {
        return ImmutableMap.of();
      }
    };
  }
}
