package se.tre.freki.storage.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

public class CassandraStoreStatements {

  private Session session;
  private final PreparedStatement createIdStatement;
  private final PreparedStatement updateNameUidStatement;
  private final PreparedStatement getNameStatement;
  private final PreparedStatement getIdStatement;
  private final PreparedStatement getMetaStatement;
  private final PreparedStatement updateMetaStatement;
  private final PreparedStatement resolveTimeSeriesStatement;

  /**
   * Instantiate the statements and prepare them with the provided session.
   *
   * @param session The session to prepare the statements with.
   */
  public CassandraStoreStatements(Session session) {

    this.createIdStatement = session.prepare(
        batch(
            insertInto(Tables.ID_TO_NAME)
                .value("label_id", bindMarker())
                .value("type", bindMarker())
                .value("creation_time", bindMarker())
                .value("name", bindMarker())
                .value("meta_description", null),
            insertInto(Tables.NAME_TO_ID)
                .value("name", bindMarker())
                .value("type", bindMarker())
                .value("creation_time", bindMarker())
                .value("label_id", bindMarker())))
        .setConsistencyLevel(ConsistencyLevel.ALL);

    this.updateNameUidStatement = session.prepare(
        batch(
            update(Tables.ID_TO_NAME)
                .with(set("name", bindMarker()))
                .where(eq("label_id", bindMarker()))
                .and(eq("type", bindMarker()))
                .and(eq("creation_time", bindMarker())),
            delete()
                .from(Tables.NAME_TO_ID)
                .where(eq("name", bindMarker()))
                .and(eq("type", bindMarker()))
                .and(eq("creation_time", bindMarker())),
            insertInto(Tables.NAME_TO_ID)
                .value("name", bindMarker())
                .value("type", bindMarker())
                .value("creation_time", bindMarker())
                .value("label_id", bindMarker())));

    this.getNameStatement = session.prepare(
        select()
            .all()
            .from(Tables.ID_TO_NAME)
            .where(eq("label_id", bindMarker()))
            .and(eq("type", bindMarker()))
            .limit(2));

    this.getIdStatement = session.prepare(
        select()
            .all()
            .from(Tables.NAME_TO_ID)
            .where(eq("name", bindMarker()))
            .and(eq("type", bindMarker()))
            .limit(2));

    this.getMetaStatement = session.prepare(
        select()
            .all()
            .from(Tables.ID_TO_NAME)
            .where(eq("label_id", bindMarker()))
            .and(eq("type", bindMarker()))
            .limit(2));

    this.updateMetaStatement = session.prepare(
        update(Tables.ID_TO_NAME)
            .with(set("meta_description", bindMarker()))
            .where(eq("label_id", bindMarker()))
            .and(eq("type", bindMarker()))
            .and(eq("creation_time", bindMarker())));

    this.resolveTimeSeriesStatement = session.prepare(
        select()
            .all()
            .from(Tables.TS_INVERTED_INDEX)
            .where(eq("label_id", bindMarker()))
            .and(eq("type", bindMarker())));

  }

  public PreparedStatement createIdStatement() {
    return createIdStatement;
  }

  public PreparedStatement updateNameUidStatement() {
    return updateMetaStatement;
  }

  public PreparedStatement getNameStatement() {
    return getNameStatement;
  }

  public PreparedStatement getIdStatement() {
    return getIdStatement;
  }

  public PreparedStatement getMetaStatement() {
    return getMetaStatement;
  }

  public PreparedStatement updateMetaStatement() {
    return updateMetaStatement;
  }

  public PreparedStatement resolveTimeSeriesStatement() {
    return resolveTimeSeriesStatement;
  }
}
