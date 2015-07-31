package se.tre.freki.utils;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncIterator {
  boolean hasMoreWithoutFetching();

  ListenableFuture<Void> fetchMore();
}
