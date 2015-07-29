package se.tre.freki.storage.cassandra.functions;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MultipleRowLoggingFunction implements Function<ResultSet, List<Row>> {

  private static final Logger LOG = LoggerFactory.getLogger(MultipleRowLoggingFunction.class);

  private final String message;
  private final Object[] param;

  public MultipleRowLoggingFunction(String logMessage, Object... param) {
    message = logMessage;
    this.param = param;
  }

  @Override
  public List<Row> apply(final ResultSet rowSet) {
    final List<Row> rows = rowSet.all();
    if (rows.size() > 1) {
      LOG.error(message, param);
    }
    return rows;
  }
}
