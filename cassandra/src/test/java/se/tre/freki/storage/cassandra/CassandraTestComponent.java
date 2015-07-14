package se.tre.freki.storage.cassandra;

import se.tre.freki.core.CoreModule;
import se.tre.freki.plugins.PluginsModule;
import se.tre.freki.storage.StoreDescriptor;
import se.tre.freki.storage.StoreModule;

import com.typesafe.config.Config;
import dagger.Component;

import javax.inject.Singleton;

/**
 * A dagger component that is configured to load a live cassandra store for use in the cassandra
 * test suites.
 */
@Component(
    modules = {
        CoreModule.class,
        PluginsModule.class,
        StoreModule.class,
        CassandraConfigModule.class
    })
@Singleton
public interface CassandraTestComponent {
  Config config();

  StoreDescriptor storeDescriptor();
}
