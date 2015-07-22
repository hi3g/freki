package se.tre.freki.storage.cassandra.statements;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FetchPointsStatementsTest {
  @Test
  public void testEnumMarkerIdOrdinal0() throws Exception {
    assertEquals(0, FetchPointsStatements.SelectPointStatementMarkers.ID.ordinal());
  }

  @Test
  public void testEnumMarkerBaseTimeOrdinal1() throws Exception {
    assertEquals(1, FetchPointsStatements.SelectPointStatementMarkers.BASE_TIME.ordinal());
  }

  @Test
  public void testEnumMarkerTimestampOrdinal2() throws Exception {
    assertEquals(2, FetchPointsStatements.SelectPointStatementMarkers.LOWER_TIMESTAMP.ordinal());
  }

  @Test
  public void testEnumMarkerValueOrdinal3() throws Exception {
    assertEquals(3, FetchPointsStatements.SelectPointStatementMarkers.UPPER_TIMESTAMP.ordinal());
  }
}
