package se.tre.freki.storage.cassandra;

import se.tre.freki.labels.LabelId;
import se.tre.freki.storage.StoreDescriptor;
import se.tre.freki.utils.InvalidConfigException;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Session;
import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
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
      return createCluster(config.getStringList("freki.storage.cassandra.nodes"),
          config.getInt("freki.storage.cassandra.protocolVersion"));
    } catch (IllegalArgumentException e) {
      throw new InvalidConfigException(config.getValue("freki.storage.cassandra.nodes"),
          "One or more of the addresses in the cassandra config could not be parsed");
    }
  }

  /**
   * Create a new cluster that is configured to use the provided list of string addresses as seed
   * nodes.
   *
   * @param nodes A list of addresses to use as seed nodes
   * @return A new {@link com.datastax.driver.core.Cluster} instance
   * @throws java.lang.IllegalArgumentException If any of the addresses could not be parsed
   */
  private Cluster createCluster(final List<String> nodes,
                                final int protocolVersion) {
    final Cluster.Builder builder = Cluster.builder();

    for (final String node : nodes) {
      final HostAndPort host = HostAndPort.fromString(node)
          .withDefaultPort(CassandraConst.DEFAULT_CASSANDRA_PORT);

      builder.addContactPoint(host.getHostText())
          .withPort(host.getPort())
          .withProtocolVersion(ProtocolVersion.fromInt(protocolVersion));
    }

    return builder.build();
  }

  Session connectTo(final Cluster cluster) {
    return cluster.connect(Tables.KEYSPACE);
  }

  @Override
  public CassandraStore createStore(final Config config,
                                    final MetricRegistry metrics) {
    final Cluster cluster = createCluster(config);
    final Session session = connectTo(cluster);
    registerMetrics(cluster, metrics);
    return new CassandraStore(cluster, session, Clock.systemDefaultZone());
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

  /**
   * Register the metrics that are exposed on the provided {@link com.datastax.driver.core.Cluster}
   * with the provided {@link com.codahale.metrics.MetricRegistry} instance.
   */
  private void registerMetrics(final Cluster cluster, final MetricRegistry metrics) {
    metrics.registerAll(cluster.getMetrics().getRegistry());
  }
}
