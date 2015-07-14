package se.tre.freki.grpc;

import se.tre.freki.core.DataPointsClient;

import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import io.grpc.ServerImpl;
import io.grpc.transport.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@Module
public class GRpcModule {
  private static final Logger LOG = LoggerFactory.getLogger(GRpcModule.class);

  @Provides
  @Singleton
  ServerImpl provideServer(final Config config,
                           final DataPointsClient dataPointsClient) {
    final int port = config.getInt("freki.grpc.port");
    LOG.info("gRPC server will listen for incoming connections on port {}", port);

    return NettyServerBuilder.forPort(port)
        .addService(DataPointAdderGrpc.bindService(new DataPointAdderImpl(dataPointsClient)))
        .build();
  }
}
