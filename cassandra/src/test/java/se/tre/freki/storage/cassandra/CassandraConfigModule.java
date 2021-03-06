package se.tre.freki.storage.cassandra;

import se.tre.freki.core.ConfigModule;

import com.typesafe.config.ConfigFactory;
import dagger.Module;

/**
 * A dagger module that inherits from the main config module but loads the cassandra test config by
 * default instead of the default "application" one.
 *
 * @see CassandraTestComponent
 */
@Module
public class CassandraConfigModule extends ConfigModule {
  public CassandraConfigModule() {
    super(ConfigFactory.load("cassandra"));
  }
}
