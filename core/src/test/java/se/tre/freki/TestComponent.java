package se.tre.freki;

import se.tre.freki.core.ConfigModule;
import se.tre.freki.core.CoreModule;
import se.tre.freki.core.LabelClientTest;
import se.tre.freki.core.MetaClientAnnotationTest;
import se.tre.freki.core.MetaClientLabelMetaTest;
import se.tre.freki.core.MetaClientTest;
import se.tre.freki.labels.LabelClientTypeContextTest;
import se.tre.freki.labels.WildcardIdLookupStrategyTest;
import se.tre.freki.plugins.PluginsModule;
import se.tre.freki.plugins.RealTimePublisher;
import se.tre.freki.search.IdChangeIndexerListenerTest;
import se.tre.freki.search.SearchPlugin;
import se.tre.freki.storage.StoreModule;
import se.tre.freki.storage.StoreModuleTest;

import dagger.Component;

import javax.inject.Singleton;

/**
 * A dagger component that is configured to be able to inject into test classes.
 *
 * <p>The module will return an instance of {@link se.tre.freki.storage.MemoryStore} but it will not
 * expose this, it is instead exposed as a general {@link se.tre.freki.storage.Store}. This detail
 * is important as we want to test a general {@link se.tre.freki.storage.Store} implementation and
 * not the behavior of the {@link se.tre.freki.storage.MemoryStore}. Because of this tests should
 * always strive to use this module as a base.
 */
@Component(
    modules = {
        ConfigModule.class,
        CoreModule.class,
        PluginsModule.class,
        StoreModule.class
    })
@Singleton
public interface TestComponent {
  void inject(MetaClientLabelMetaTest metaClientLabelMetaTest);

  void inject(StoreModuleTest storeModuleTest);

  void inject(LabelClientTypeContextTest labelClientTypeContextTest);

  void inject(LabelClientTest idClientTest);

  void inject(MetaClientAnnotationTest metaClientAnnotationTest);

  void inject(IdChangeIndexerListenerTest idChangeIndexerListenerTest);

  void inject(WildcardIdLookupStrategyTest wildcardIdLookupStrategyTest);

  void inject(MetaClientTest metaClientTest);

  SearchPlugin searchPlugin();

  RealTimePublisher realTimePublisher();
}
