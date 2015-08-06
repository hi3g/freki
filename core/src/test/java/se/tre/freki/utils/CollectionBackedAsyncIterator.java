package se.tre.freki.utils;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * An implementation of the {@link AsyncIterator} that is backed by an immutable collection for use
 * in tests.
 *
 * @param <E> The type of elements this iterator will iterate over
 */
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
  public ListenableFuture<Boolean> fetchMore() {
    return Futures.immediateFuture(hasNext());
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
