package se.tre.freki.stats;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

public class MetricsTest {
  @Test(expected = IllegalArgumentException.class)
  public void testMetricInEmptyString() throws Exception {
    Metrics.metricIn("");
  }

  @Test(expected = IllegalStateException.class)
  public void testMetricInOnlyColon() throws Exception {
    Metrics.metricIn(":");
  }

  @Test(expected = IllegalStateException.class)
  public void testMetricInOnlyAfterColon() throws Exception {
    Metrics.metricIn(":tagk=tagv");
  }

  @Test
  public void testMetricInReturnsMetric() throws Exception {
    assertEquals("metric", Metrics.metricIn("metric:"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTagsInEmptyString() throws Exception {
    Metrics.tagsIn("");
  }

  @Test(expected = IllegalStateException.class)
  public void testTagsInOnlyColon() throws Exception {
    Metrics.tagsIn(":");
  }

  @Test(expected = IllegalStateException.class)
  public void testTagsInOnlyBeforeColon() throws Exception {
    Metrics.tagsIn("metric:");
  }

  @Test
  public void testTagsInOnlyAfterColon() throws Exception {
    final Map<String, String> expected = ImmutableMap.of("tagk", "tagv");
    assertEquals(expected, Metrics.tagsIn(":tagk=tagv"));
  }
}
