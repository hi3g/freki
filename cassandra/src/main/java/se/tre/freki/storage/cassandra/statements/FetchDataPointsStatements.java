package se.tre.freki.storage.cassandra.statements;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import se.tre.freki.storage.cassandra.Tables;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

public class FetchDataPointsStatements {
  private final PreparedStatement selectDataPointsStatement;

  /**
   * Instantiate the statements and prepare them with the provided session.
   *
   * @param session The session to prepare the statements with.
   */
  public FetchDataPointsStatements(final Session session) {
    selectDataPointsStatement = session.prepare(
        select()
            .all()
            .from(Tables.DATAPOINTS)
            .where(eq("timeseries_id", bindMarker()))
            .and(gte("basetime", bindMarker()))
            .and(lte("basetime", bindMarker()))
            .and(gte("timestamp", bindMarker()))
            .and(lte("timestamp", bindMarker())));
  }

  public PreparedStatement selectDataPointsStatement() {
    return selectDataPointsStatement;
  }
}
