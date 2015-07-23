package se.tre.freki.storage.cassandra;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Iterator;
import java.util.List;

public class ExhaustedResultSet implements ResultSet {
  @Override
  public ColumnDefinitions getColumnDefinitions() {
    throw new UnsupportedOperationException("No execution has been made");
  }

  @Override
  public boolean isExhausted() {
    return true;
  }

  @Override
  public Row one() {
    return null;
  }

  @Override
  public List<Row> all() {
    return ImmutableList.of();
  }

  @Override
  public Iterator<Row> iterator() {
    return all().iterator();
  }

  @Override
  public int getAvailableWithoutFetching() {
    return 0;
  }

  @Override
  public boolean isFullyFetched() {
    return true;
  }

  @Override
  public ListenableFuture<Void> fetchMoreResults() {
    return Futures.immediateFuture(null);
  }

  @Override
  public ExecutionInfo getExecutionInfo() {
    throw new UnsupportedOperationException("No execution has been made");
  }

  @Override
  public List<ExecutionInfo> getAllExecutionInfo() {
    throw new UnsupportedOperationException("No execution has been made");
  }

  @Override
  public boolean wasApplied() {
    return true;
  }
}
