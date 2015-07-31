package se.tre.freki.core;

import se.tre.freki.labels.IdLookupStrategy;
import se.tre.freki.stats.InternalMetricRegistrator;
import se.tre.freki.storage.Store;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

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
                                       final LabelClient labelClient,
                                       final DataPointsClient dataPointsClient,
                                       final InternalMetricRegistrator metricRegistrator) {
    final MetricRegistry registry = new MetricRegistry();
    registry.addListener(metricRegistrator);

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
  IdLookupStrategy provideIdLookupStrategy() {
    return IdLookupStrategy.CreatingIdLookupStrategy.instance;
  }
}
