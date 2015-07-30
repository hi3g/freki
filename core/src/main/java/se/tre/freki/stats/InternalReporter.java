package se.tre.freki.stats;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

import java.util.SortedMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InternalReporter extends ScheduledReporter {
  public InternalReporter(final MetricRegistry registry,
                          final String name,
                          final MetricFilter filter,
                          final TimeUnit rateUnit,
                          final TimeUnit durationUnit) {
    super(registry, name, filter, rateUnit, durationUnit);
  }

  public InternalReporter(final MetricRegistry registry,
                          final String name,
                          final MetricFilter filter,
                          final TimeUnit rateUnit,
                          final TimeUnit durationUnit,
                          final ScheduledExecutorService executor) {
    super(registry, name, filter, rateUnit, durationUnit, executor);
  }

  @Override
  public void report(final SortedMap<String, Gauge> gauges,
                     final SortedMap<String, Counter> counters,
                     final SortedMap<String, Histogram> histograms,
                     final SortedMap<String, Meter> meters,
                     final SortedMap<String, Timer> timers) {

  }
}
