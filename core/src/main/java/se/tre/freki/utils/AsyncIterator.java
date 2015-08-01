package se.tre.freki.utils;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Iterator;

public interface AsyncIterator<E> extends Iterator<E> {
  boolean hasMoreWithoutFetching();

  ListenableFuture<Void> fetchMore();
}
