package se.tre.freki.query.functions;

import se.tre.freki.query.DataPoint;
import se.tre.freki.utils.AsyncIterator;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.NoSuchElementException;

public abstract class QueryFunction<D extends DataPoint> implements AsyncIterator<D> {
  private final AsyncIterator<? extends DataPoint> iterator;

  QueryFunction(final AsyncIterator<? extends DataPoint> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasMoreWithoutFetching() {
    return iterator.hasMoreWithoutFetching();
  }

  @Override
  public ListenableFuture<Boolean> fetchMore() {
    return iterator.fetchMore();
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  protected void checkHasNext(final String message) {
    if (!hasNext()) {
      throw new NoSuchElementException(message);
    }
  }

  public AsyncIterator<? extends DataPoint> iterator() {
    return iterator;
  }
}
