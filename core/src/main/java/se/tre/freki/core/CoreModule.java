package se.tre.freki.core;

import se.tre.freki.stats.FrekiMetricRegistrator;
import se.tre.freki.stats.FrekiMetricReporter;
import se.tre.freki.storage.Store;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

@Module
public class CoreModule {
  @Provides
  @Singleton
  EventBus provideEventBus() {
    return new EventBus();
  }

  @Provides
  @Singleton
  MetricRegistry provideMetricRegistry(final Store store,
                                       final Clock clock,
                                       final LabelClient labelClient,
                                       final DataPointsClient dataPointsClient,
                                       final FrekiMetricRegistrator metricRegistrator) {
    final MetricRegistry registry = new MetricRegistry();
    registry.addListener(metricRegistrator);

    final FrekiMetricReporter frekiMetricReporter = new FrekiMetricReporter(clock, registry,
        dataPointsClient, defaultTags());
    frekiMetricReporter.start(30, TimeUnit.SECONDS);

    registry.registerAll(new ClassLoadingGaugeSet());
    registry.registerAll(new GarbageCollectorMetricSet());
    registry.registerAll(new MemoryUsageGaugeSet());
    registry.registerAll(new ThreadStatesGaugeSet());
    registry.register("descriptor-usage", new FileDescriptorRatioGauge());

    store.registerMetricsWith(registry);
    labelClient.registerMetricsWith(registry);
    dataPointsClient.registerMetricsWith(registry);

    return registry;
  }

  @Provides
  @Singleton
  FrekiMetricRegistrator provideInternalMetricRegistrator(
      final LabelClient labelClient) {
    return new FrekiMetricRegistrator(labelClient, defaultTags());
  }

  @Provides
  public Clock provideClock() {
    return Clock.systemDefaultZone();
  }

  /**
   * Default tags used on all metrics reported internally using the {@link FrekiMetricReporter}.
   *
   * @return A map that contains all the default tags
   */
  private ImmutableMap<String, String> defaultTags() {
    try {
      final String hostName = InetAddress.getLocalHost().getHostName();
      return ImmutableMap.of("host", hostName);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Unable to lookup hostname for localhost");
    }
  }
}
