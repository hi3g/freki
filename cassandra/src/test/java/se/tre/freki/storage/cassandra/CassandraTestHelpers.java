package se.tre.freki.storage.cassandra;


import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Utility methods that are useful for testing the cassandra store.
 */
public class CassandraTestHelpers {
  /**
   * A default timeout in milliseconds to wait for Cassandra in tests.
   */
  public static final long TIMEOUT = TimeUnit.MILLISECONDS.convert(15, TimeUnit.SECONDS);

  /**
   * Clear the data in all tables.
   *
   * @param session A live session to talk to
   */
  public static void truncate(final Session session) {
    session.execute(QueryBuilder.truncate(Tables.DATAPOINTS));
    session.execute(QueryBuilder.truncate(Tables.TS_INVERTED_INDEX));
    session.execute(QueryBuilder.truncate(Tables.ID_TO_NAME));
    session.execute(QueryBuilder.truncate(Tables.NAME_TO_ID));
    session.execute(QueryBuilder.truncate(Tables.LABEL_SEARCH_INDEX));
    session.execute(QueryBuilder.truncate(Tables.META));
  }
}
