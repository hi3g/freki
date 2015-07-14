
package se.tre.freki.grpc;

import se.tre.freki.application.CommandLineApplication;
import se.tre.freki.application.CommandLineOptions;
import se.tre.freki.utils.InvalidConfigException;

import com.google.common.io.Closeables;
import com.typesafe.config.ConfigException;
import io.grpc.ServerImpl;
import joptsimple.OptionException;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.inject.Inject;

/**
 * Command line tool to run a low latency protocol buffers server that accepts data points.
 */
public final class GRpcApplication {
  private static final Logger LOG = LoggerFactory.getLogger(GRpcApplication.class);

  private ServerImpl server;

  @Inject
  GRpcApplication(final ServerImpl server) {
    this.server = server;
  }

  /**
   * Entry-point for the gRPC application. The gRPC program is normally not executed directly but
   * rather through the main project.
   *
   * @param args The command-line arguments
   */
  public static void main(final String[] args) {
    // Release an FD that we don't need or want
    Closeables.closeQuietly(System.in);

    final CommandLineApplication application = CommandLineApplication.builder()
        .command("grpc")
        .description("Start the protocol buffers server")
        .helpText("The server will starting listening on the configured port and accept data points"
                  + "sent as gRPC messages.")
        .usage("[OPTIONS]")
        .build();

    final CommandLineOptions cmdOptions = new CommandLineOptions();

    try {
      final OptionSet options = cmdOptions.parseOptions(args);

      if (options.has("help")) {
        application.printHelpAndExit(cmdOptions);
      }

      cmdOptions.configureLogger();

      final GrpcComponent grpcComponent = DaggerGrpcComponent.builder()
          .configModule(cmdOptions.configModule())
          .build();

      final GRpcApplication server = grpcComponent.server();
      server.start();

    } catch (IllegalArgumentException | OptionException e) {
      application.printError(e.getMessage());
      System.exit(42);
    } catch (InvalidConfigException | ConfigException e) {
      System.err.println(e.getMessage());
      System.exit(42);
    } catch (Exception e) {
      LOG.error("Fatal error while assigning id", e);
      System.exit(42);
    }
  }

  /**
   * Start listening for connections. Should only be called once.
   *
   * @throws IOException if the server had already been started
   */
  private void start() throws IOException {
    server.start();

    LOG.info("gRPC server started and is now listening for incoming connections");

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        GRpcApplication.this.stop();
      }
    });
  }

  /**
   * Stop the server. This is a no-op if the server has not been started yet.
   */
  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }
}
