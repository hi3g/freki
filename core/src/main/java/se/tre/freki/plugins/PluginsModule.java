package se.tre.freki.plugins;

import se.tre.freki.search.SearchPlugin;
import se.tre.freki.search.SearchPluginDescriptor;
import se.tre.freki.storage.Store;

import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class PluginsModule {
  @Provides
  @Singleton
  SearchPlugin provideSearchPlugin(final Config config, final Store store) {
    try {
      SearchPluginDescriptor descriptor = PluginLoader.loadPluginWithName(
          config.getString("freki.search.plugin"),
          SearchPluginDescriptor.class);

      return descriptor.create(config, store);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to instantiate the configured search plugin", e);
    }
  }

  @Provides
  @Singleton
  RealTimePublisher provideRealtimePublisher(final Config config) {
    try {
      RealTimePublisherDescriptor descriptor = PluginLoader.loadPluginWithName(
          config.getString("freki.publisher.plugin"),
          RealTimePublisherDescriptor.class);

      return descriptor.create(config);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to instantiate the configured realtime publisher", e);
    }
  }
}
