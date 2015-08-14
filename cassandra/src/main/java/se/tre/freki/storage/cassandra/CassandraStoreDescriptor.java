package se.tre.freki.storage.cassandra;

import se.tre.freki.labels.LabelId;
import se.tre.freki.storage.StoreDescriptor;
import se.tre.freki.utils.InvalidConfigException;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Session;
import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;

import java.time.Clock;
import java.util.List;
import javax.annotation.Nonnull;

@AutoService(StoreDescriptor.class)
public class CassandraStoreDescriptor extends StoreDescriptor {
  /**
   * Create a new cluster that is configured to use the list of addresses in the config as seed
   * nodes.
   *
   * @param config The config to get the list of addresses to from
   * @return A new {@link com.datastax.driver.core.Cluster} instance
   * @throws com.typesafe.config.ConfigException if the config key was missing or is malformed
   * @throws InvalidConfigException if one of the addresses could not be parsed
   */
  @VisibleForTesting
  Cluster createCluster(final Config config) {
    try {
      final int port = config.getInt("freki.storage.cassandra.port");
      final List<String> nodes = config.getStringList("freki.storage.cassandra.nodes");
      final int protocolVersion = config.getInt("freki.storage.cassandra.protocolVersion");

      final Cluster.Builder builder = Cluster.builder();

      for (final String node : nodes) {
        builder.addContactPoint(node);
      }

      builder.withPort(port)
          .withProtocolVersion(ProtocolVersion.fromInt(protocolVersion));

      return builder.build();
    } catch (IllegalArgumentException e) {
      throw new InvalidConfigException(config.getValue("freki.storage.cassandra.nodes"),
          "One or more of the addresses in the cassandra config could not be parsed", e);
    }
  }

  Session connectTo(final Cluster cluster, String keyspace) {
    return cluster.connect(keyspace);
  }

  @Override
  public CassandraStore createStore(final Config config) {
    final Cluster cluster = createCluster(config);
    final String keyspace = config.getString("freki.storage.cassandra.keyspace");
    final Session session = connectTo(cluster, keyspace);

    final IndexStrategy addPointIndexStrategy = indexStrategyFor(session,
        config.getBoolean("freki.storage.cassandra.index_on_add_point"));

    return new CassandraStore(cluster, session, Clock.systemDefaultZone(), addPointIndexStrategy);
  }

  @Nonnull
  @Override
  public LabelId.LabelIdSerializer labelIdSerializer() {
    return new CassandraLabelId.CassandraLabelIdSerializer();
  }

  @Nonnull
  @Override
  public LabelId.LabelIdDeserializer labelIdDeserializer() {
    return new CassandraLabelId.CassandraLabelIdDeserializer();
  }

  @Nonnull
  private IndexStrategy indexStrategyFor(final Session session,
                                         final boolean shouldWrite) {
    if (shouldWrite) {
      return new IndexStrategy.IndexingStrategy(session);
    }

    return new IndexStrategy.NoOpIndexingStrategy();
  }
}
