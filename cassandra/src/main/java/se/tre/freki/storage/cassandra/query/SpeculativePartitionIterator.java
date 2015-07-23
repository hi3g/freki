package se.tre.freki.storage.cassandra.query;

import se.tre.freki.query.QueryException;
import se.tre.freki.storage.cassandra.ExhaustedResultSet;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

public class SpeculativePartitionIterator<K> implements Iterator<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(SpeculativePartitionIterator.class);

  private final Function<K, ResultSetFuture> fetchFunction;
  private final Iterator<K> partitionKeyGenerator;

  private ResultSet currentResultSet;
  private ListenableFuture<ResultSet> nextResultSet;

  public SpeculativePartitionIterator(final Iterator<K> partitionKeyGenerator,
                                      final Function<K, ResultSetFuture> fetchFunction) {
    this.partitionKeyGenerator = partitionKeyGenerator;
    this.fetchFunction = fetchFunction;

    currentResultSet = new ExhaustedResultSet();
    nextResultSet = fetchNextPartition();
  }

  @Override
  public boolean hasNext() {
    if (currentResultSet.isExhausted()) {
      currentResultSet = nextPartitionResultSet();
      return !currentResultSet.isExhausted();
    }

    return true;
  }

  @Override
  public Row next() {
    checkHasNext("There are no more rows to iterate over");
    return currentResultSet.one();
  }

  private void checkHasNext(final String message) {
    if (!hasNext()) {
      throw new NoSuchElementException(message);
    }
  }

  private ResultSet nextPartitionResultSet() {
    try {
      if (nextResultSet.isDone()) {
        final ListenableFuture<ResultSet> fetchedResultSet = nextResultSet;
        nextResultSet = fetchNextPartition();
        return fetchedResultSet.get();
      }

      LOG.info("Waiting for next partition to finish loading");
      return nextResultSet.get();
    } catch (ExecutionException e) {
      throw new QueryException("Fetch of next partition threw an exception", e.getCause());
    } catch (InterruptedException e) {
      throw new QueryException("Interrupted while waiting for next partition");
    }
  }

  private ListenableFuture<ResultSet> fetchNextPartition() {
    if (!partitionKeyGenerator.hasNext()) {
      return Futures.immediateFuture(new ExhaustedResultSet());
    }

    return fetchFunction.apply(partitionKeyGenerator.next());
  }
}
