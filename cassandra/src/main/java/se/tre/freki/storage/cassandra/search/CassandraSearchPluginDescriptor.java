package se.tre.freki.storage.cassandra.search;

import static com.google.common.base.Preconditions.checkArgument;

import se.tre.freki.search.SearchPlugin;
import se.tre.freki.search.SearchPluginDescriptor;
import se.tre.freki.storage.Store;
import se.tre.freki.storage.cassandra.CassandraStore;

import com.typesafe.config.Config;

public class CassandraSearchPluginDescriptor extends SearchPluginDescriptor {
  @Override
  public SearchPlugin create(final Config config, final Store store) throws Exception {
    checkArgument(store instanceof CassandraStore,
        "The %s can only be used with the %s", CassandraSearchPlugin.class, CassandraStore.class);

    CassandraStore cassandraStore = (CassandraStore) store;

    return new CassandraSearchPlugin(cassandraStore);
  }
}
