package se.tre.freki.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

public class EventLoopGroups {
  private static final Logger LOG = LoggerFactory.getLogger(EventLoopGroups.class);

  private static EventLoopGroup bossGroup;
  private static EventLoopGroup workerGroup;

  private EventLoopGroups() {
  }

  /**
   * Get a reference to the shared global boss {@link EventLoopGroup}. The hinted parallelism is
   * only respected for the first call.
   *
   * @param hintedParallelism The parallelism the callee wishes the returned Netty event loop group
   * to have.
   * @return A configured Netty event loop group for use as a boss group.
   */
  public static synchronized EventLoopGroup sharedBossGroup(int hintedParallelism) {
    if (bossGroup == null) {
      bossGroup = newLoopGroup("boss", hintedParallelism);
    }

    return bossGroup;
  }

  /**
   * Get a reference to the shared global worker {@link EventLoopGroup}. The hinted parallelism is
   * only respected for the first call.
   *
   * @param hintedParallelism The parallelism the callee wishes the returned Netty event loop group
   * to have.
   * @return A configured Netty event loop group for use as a worker group.
   */
  public static synchronized EventLoopGroup sharedWorkerGroup(int hintedParallelism) {
    if (workerGroup == null) {
      workerGroup = newLoopGroup("worker", hintedParallelism);
    }

    return workerGroup;
  }

  /**
   * Create a new Netty event loop group whose threads will have the provided base name and the
   * provided parallelism.
   *
   * @param baseName The basename of the threads used by the new event loop group
   * @param parallelism The parallelism the new event loop group should have
   * @return An initialized event loop group who will be shutdown by a shutdown hook
   */
  private static EventLoopGroup newLoopGroup(final String baseName, final int parallelism) {
    EventLoopGroup workerGroup = new EpollEventLoopGroup(parallelism, threadFactory(baseName));
    Runtime.getRuntime().addShutdownHook(shutdownHookFor(workerGroup, baseName));
    LOG.info("Created event loop group with base name {} and parallelism {}",
        baseName, parallelism);
    return workerGroup;
  }

  /**
   * Create a new thread factory that will create threads with the provided base name.
   *
   * @param baseName The base name the threads provided by this thread factory will have
   * @return A new thread factory that creates threads with the provided base name
   */
  private static ThreadFactory threadFactory(final String baseName) {
    return new ThreadFactoryBuilder()
        .setNameFormat(baseName + "-%d")
        .setDaemon(false)
        .setPriority(Thread.NORM_PRIORITY)
        .build();
  }

  /**
   * Create a new thread that can be used as a shutdown hook. When the thread is started it will
   * call {@link EventLoopGroup#shutdownGracefully()} on it.
   *
   * @param eventLoopGroup The event loop group to shutdown
   * @return A new not started thread that will shutdown the provided event loop group when started
   */
  private static Thread shutdownHookFor(final EventLoopGroup eventLoopGroup,
                                        final String baseName) {
    return new Thread(null, new EventLoopGroupShutdownHook(eventLoopGroup), baseName + "-shutdown");
  }

  /**
   * A runnable class that will shutdown {@link EventLoopGroup}s when ran.
   *
   * @see #shutdownHookFor(EventLoopGroup, String)
   */
  private static class EventLoopGroupShutdownHook implements Runnable {
    private final EventLoopGroup eventLoopGroup;

    public EventLoopGroupShutdownHook(final EventLoopGroup eventLoopGroup) {
      this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public void run() {
      try {
        eventLoopGroup.shutdownGracefully().sync();
      } catch (InterruptedException e) {
        // We can not use the logging framework since it may either be in the process of shutting
        // down or it may already have done so. This will print to stderr which is good enough.
        e.printStackTrace();
      }
    }
  }
}
