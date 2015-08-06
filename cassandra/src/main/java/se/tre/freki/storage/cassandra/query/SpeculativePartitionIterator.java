package se.tre.freki.storage.cassandra.query;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.transform;

import se.tre.freki.query.QueryException;
import se.tre.freki.storage.cassandra.ExhaustedResultSet;
import se.tre.freki.storage.cassandra.functions.TrueFunction;
import se.tre.freki.utils.AsyncIterator;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

/**
 * An iterator implementation that speculatively fetches the next partition when it detects that the
 * current one is close to being exhausted. Whether there are any more elements to get without
 * fetching can be checked by calling {@link #hasMoreWithoutFetching()} and {@link #fetchMore()} can
 * be called without blocking to get a future that {@link ListenableFuture#isDone()} once the fetch
 * is complete. The future returned by {@link #fetchMore()} will contain a {@code boolean} that
 * indicates if there is anything more to read or not.
 *
 * <p>The partition keys are generated by the provided iterator and the partitions are fetched using
 * the provided fetch function.
 *
 * @param <K> The type of the partition key
 */
public class SpeculativePartitionIterator<K> implements AsyncIterator<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(SpeculativePartitionIterator.class);

  private ListenableFuture<ResultSet> exhaustedNextResultSet;

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

    final ResultSet exhaustedResultSet = new ExhaustedResultSet();
    this.exhaustedNextResultSet = immediateFuture(exhaustedResultSet);

    currentResultSet = exhaustedResultSet;
    nextResultSet = fetchNextPartition();
  }

  @Override
  public boolean hasMoreWithoutFetching() {
    return currentResultSet.getAvailableWithoutFetching() > 0;
  }

  @Override
  public ListenableFuture<Boolean> fetchMore() {
    if (currentResultSet.isExhausted()) {
      return transform(nextPartitionResultSet(), new AsyncFunction<ResultSet, Boolean>() {
        @Override
        public ListenableFuture<Boolean> apply(final ResultSet fetchedResultSet) {
          currentResultSet = fetchedResultSet;

          if (currentResultSet.isExhausted()) {
            if (nextResultSet != exhaustedNextResultSet) {
              return fetchMore();
            }

            return immediateFuture(Boolean.FALSE);
          }

          return immediateFuture(Boolean.TRUE);
        }
      });
    } else if (currentResultSet.getAvailableWithoutFetching() == 0) {
      return transform(currentResultSet.fetchMoreResults(), new TrueFunction());
    }

    return immediateFuture(Boolean.TRUE);
  }

  @Override
  public boolean hasNext() {
    if (currentResultSet.isExhausted()) {
      try {
        LOG.debug("Waiting for next partition {} to finish loading", nextResultSet);
        return fetchMore().get();
      } catch (ExecutionException e) {
        throw new QueryException("Fetch of next partition threw an exception", e);
      } catch (InterruptedException e) {
        throw new QueryException("Interrupted while waiting for next partition", e);
      }
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
   * Start fetching the next partition and return the future of the partition currently being
   * fetched.
   *
   * @return The value of {@link #nextResultSet} as it was before this method was called
   */
  private ListenableFuture<ResultSet> nextPartitionResultSet() {
    final ListenableFuture<ResultSet> fetchedResultSet = nextResultSet;
    nextResultSet = fetchNextPartition();
    return fetchedResultSet;
  }

  /**
   * Start fetching the next partition. If there are no more partitions to fetch an immidiate future
   * will be returned with an exhausted result set.
   *
   * @return A future that on completion will contain the result set of the next partition
   */
  private ListenableFuture<ResultSet> fetchNextPartition() {
    if (!partitionKeyGenerator.hasNext()) {
      return exhaustedNextResultSet;
    }

    final K nextPartitionKey = partitionKeyGenerator.next();
    final ResultSetFuture nextPartition = fetchFunction.apply(nextPartitionKey);

    LOG.trace("Initiated load of next partition in {} with key {}",
        nextPartition, nextPartitionKey);
    return nextPartition;
  }
}
