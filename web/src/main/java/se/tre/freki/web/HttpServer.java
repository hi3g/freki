package se.tre.freki.web;

import se.tre.freki.application.CommandLineApplication;
import se.tre.freki.application.CommandLineOptions;
import se.tre.freki.utils.EventLoopGroups;
import se.tre.freki.utils.InvalidConfigException;

import com.google.common.io.Closeables;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import joptsimple.OptionException;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpServer {
  private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

  /**
   * Entry-point for the http server application. The web program is normally not executed
   * directly but rather through the main project.
   *
   * @param args The command-line arguments
   */
  public static void main(String[] args) {
    // Release an FD that we don't need or want
    Closeables.closeQuietly(System.in);

    final CommandLineApplication cliApplication = CommandLineApplication.builder()
        .command("web")
        .description("Start the REST API server")
        .helpText("The REST server will respond to regular HTTP 1.1 requests")
        .usage("[OPTIONS]")
        .build();

    final CommandLineOptions cmdOptions = new CommandLineOptions();

    try {
      final OptionSet options = cmdOptions.parseOptions(args);

      if (options.has("help")) {
        cliApplication.printHelpAndExit(cmdOptions);
      }

      cmdOptions.configureLogger();

      HttpServerComponent httpServerComponent = DaggerHttpServerComponent.builder()
          .configModule(cmdOptions.configModule())
          .build();

      final Config config = httpServerComponent.config();

      final EventLoopGroup bossGroup = EventLoopGroups.sharedBossGroup(
          config.getInt("freki.web.threads.boss_group"));
      final EventLoopGroup workerGroup = EventLoopGroups.sharedWorkerGroup(
          config.getInt("freki.web.threads.worker_group"));

      try {
        final ServerBootstrap b = new ServerBootstrap()
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.SO_BACKLOG, config.getInt("freki.web.backlog"))
            .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
            .option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
            .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
            .group(bossGroup, workerGroup)
            .channel(EpollServerSocketChannel.class)
            .handler(new LoggingHandler())
            .childHandler(httpServerComponent.httpServerInitializer());

        final int listenPort = config.getInt("freki.web.port");
        final ChannelFuture bindFuture = b.bind(listenPort).sync();
        LOG.info("Web server is now listening on port {}", listenPort);
        bindFuture.channel().closeFuture().sync();
      } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
      }
    } catch (IllegalArgumentException | OptionException | InterruptedException e) {
      cliApplication.printError(e.getMessage());
      System.exit(42);
    } catch (InvalidConfigException | ConfigException e) {
      System.err.println(e.getMessage());
      System.exit(42);
    }
  }
}
