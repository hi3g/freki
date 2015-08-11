package se.tre.freki.storage.cassandra;

import se.tre.freki.utils.EventLoopGroups;

import com.datastax.driver.core.NettyOptions;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.ThreadFactory;

class CassandraNettyOptions extends NettyOptions {
  @Override
  public EventLoopGroup eventLoopGroup(final ThreadFactory threadFactory) {
    return EventLoopGroups.sharedWorkerGroup(4);
  }

  @Override
  public Class<? extends SocketChannel> channelClass() {
    return EpollSocketChannel.class;
  }

  @Override
  public void onClusterClose(final EventLoopGroup eventLoopGroup) {
    // Do nothing to prevent the event loop groups shutdown
  }
}
