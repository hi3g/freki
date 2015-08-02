package se.tre.freki.utils;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class CollectionBackedAsyncIterator<E> implements AsyncIterator<E> {
  private final UnmodifiableIterator<E> iterator;

  public CollectionBackedAsyncIterator(final ImmutableCollection<E> elements) {
    this.iterator = elements.iterator();
  }

  @Override
  public boolean hasMoreWithoutFetching() {
    return hasNext();
  }

  @Override
  public ListenableFuture<Void> fetchMore() {
    return Futures.immediateFuture(null);
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public E next() {
    return iterator.next();
  }
}
