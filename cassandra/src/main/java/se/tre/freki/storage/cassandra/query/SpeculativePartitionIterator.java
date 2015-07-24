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

/**
 * An iterator implementation that speculatively loads the next partition when it detects that the
 * current one is close to being exhausted.
 *
 * <p>The partition keys are generated by the provided iterator and the partitions are fetched using
 * the provided fetch function.
 *
 * @param <K> The type of the partition key
 */
public class SpeculativePartitionIterator<K> implements Iterator<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(SpeculativePartitionIterator.class);

  private final Function<K, ResultSetFuture> fetchFunction;
  private final Iterator<K> partitionKeyGenerator;

  private ResultSet currentResultSet;
  private ListenableFuture<ResultSet> nextResultSet;

  /**
   * Create a new iterator that reads partition keys from the provided {@link
   * #partitionKeyGenerator} and loads the partitions using the {@link #fetchFunction}.
   *
   * @param partitionKeyGenerator An iterator that generates partition keys
   * @param fetchFunction A function that loads partitions given the partition keys generated by the
   * provided iterator
   */
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

  /**
   * Blocks and waits for {@link #nextResultSet} to be done and starts fetching the next partition.
   *
   * @return The result set of the partition being loaded by {@link #nextResultSet}
   * @throws QueryException if there was a problem when fetching the partition or if the curent
   * thread was interrupted
   */
  private ResultSet nextPartitionResultSet() {
    try {
      final ListenableFuture<ResultSet> fetchedResultSet = nextResultSet;
      nextResultSet = fetchNextPartition();

      if (!fetchedResultSet.isDone()) {
        LOG.debug("Waiting for next partition {} to finish loading", nextResultSet);
      } else {
        LOG.trace("Load of next partition {} already finished", nextResultSet);
      }

      return fetchedResultSet.get();
    } catch (ExecutionException e) {
      throw new QueryException("Fetch of next partition threw an exception", e.getCause());
    } catch (InterruptedException e) {
      throw new QueryException("Interrupted while waiting for next partition");
    }
  }

  /**
   * Start fetching the next partition. If there are no more partitions to fetch an immidiate future
   * will be returned with an exhausted result set.
   *
   * @return A future that on completion will contain the result set of the next partition
   */
  private ListenableFuture<ResultSet> fetchNextPartition() {
    if (!partitionKeyGenerator.hasNext()) {
      LOG.trace("Told to fetch the next partition but partition key generator is exhausted");
      return Futures.immediateFuture(new ExhaustedResultSet());
    }

    final K nextPartitionKey = partitionKeyGenerator.next();
    final ResultSetFuture nextPartition = fetchFunction.apply(nextPartitionKey);

    LOG.trace("Initiated load of next partition in {} with key {}",
        nextPartition, nextPartitionKey);
    return nextPartition;
  }
}
