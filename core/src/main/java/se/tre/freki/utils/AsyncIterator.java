package se.tre.freki.utils;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Iterator;

/**
 * An extension of the {@link Iterator} interface that provides methods for checking whether more
 * elements can be gotten without blocking for a prolonged duration and a method to explicitly ask
 * for a handle to be notified when there are more.
 *
 * @param <E> The type of elements this iterator will iterate over
 */
public interface AsyncIterator<E> extends Iterator<E> {
  /**
   * Returns {@code true} if the iterator has more elements to iterate over without performing a
   * blocking wait for a prolonged duration. This method may return {@code false} early in which
   * case one most likely will want to call {@link #fetchMore()} but it should always return {@code
   * false} when {@link #hasNext()} does return so.
   *
   * @return {@code true} if more elements can be iterated over without blocking for a prolonged
   * duration
   */
  boolean hasMoreWithoutFetching();

  /**
   * Fetch more elements to iterate over and return a future that completes once new elements are
   * available.
   *
   * @return A future that is done once new elements are available
   */
  ListenableFuture<Void> fetchMore();
}
