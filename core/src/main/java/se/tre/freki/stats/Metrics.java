package se.tre.freki.stats;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Instead of juggling a registry around directly we use this class. It might be really useful in
 * the future to store {@link com.codahale.metrics.Metric}s on.
 */
public class Metrics {
  private Metrics() {
  }

  /**
   * Check if the provided name is an "internal" metric. All metrics starting with the string
   * "freki" are considered to be "internal".
   *
   * @param name The name to check
   * @return {@code true} if it is an internal metric, otherwise {@code false}.
   */
  static boolean isFrekiName(final String name) {
    return name.startsWith("freki");
  }

  /**
   * Create a new name for the metrics library with the given metric and tags.
   */
  public static String name(String metric, Tag... tags) {
    StringBuilder sb = new StringBuilder()
        .append(metric);

    if (tags.length > 0) {
      sb.append(':');
      Joiner.on(",").appendTo(sb, tags);
    }

    return sb.toString();
  }

  /**
   * Create a new tag with the given tag key and tag value. You should only rely on this method for
   * anything else other than for use with the name method above.
   */
  public static Tag tag(final String key, final String value) {
    return new Tag(key, value);
  }

  /**
   * Inner class that describes tags for use with the name method above. You should not rely on this
   * class for anything else.
   */
  public static class Tag {
    public final String key;
    public final String value;

    public Tag(final String key, final String value) {
      this.key = checkNotNull(key);
      this.value = checkNotNull(value);
    }

    @Override
    public String toString() {
      return key + '=' + value;
    }
  }

  /**
   * Extract the Freki metric contained in the provided metrics core metric name.
   *
   * @see #name(String, Tag...)
   */
  static String metricIn(final String name) {
    checkArgument(!name.isEmpty());
    final String metric = name.split(":", 2)[0];
    checkState(!metric.isEmpty(), "The provided name (%s) did not contain a metric", name);
    return metric;
  }

  /**
   * Extract the Freki tags contained in the provided metrics core metric name.
   *
   * @see #name(String, Tag...)
   */
  static Map<String, String> tagsIn(final String name) {
    checkArgument(!name.isEmpty());
    final String[] parts = name.split(":", 2);

    if (parts.length == 2) {
      final String tags = name.split(":", 2)[1];
      checkState(!tags.isEmpty(), "The provided name (%s) did not contain any tags", name);

      return new LinkedHashMap<>(Splitter.on(',')
          .withKeyValueSeparator('=')
          .split(tags));
    }

    return new LinkedHashMap<>();
  }
}
