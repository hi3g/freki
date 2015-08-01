package se.tre.freki.storage.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;
import static se.tre.freki.storage.cassandra.CassandraLabelId.makeIdTypeSpecific;

import se.tre.freki.labels.LabelId;

import com.google.common.hash.Hashing;
import org.junit.Test;

public class CassandraLabelIdTest {
  private static final long SMALL_ID = 3;
  private static final long LARGE_ID = 524291;

  private static final long LARGE_ID_METRIC = 524288;
  private static final long LARGE_ID_TAG_KEY = 524290;
  private static final long LARGE_ID_TAG_VALUE = 524289;

  @Test
  public void testEqualsSameId() throws Exception {
    final LabelId firstId = CassandraLabelId.fromLong(SMALL_ID);
    final LabelId secondId = CassandraLabelId.fromLong(SMALL_ID);
    assertTrue(firstId.equals(secondId));
  }

  @Test
  public void testEqualsNotSameId() throws Exception {
    final LabelId firstId = CassandraLabelId.fromLong(LARGE_ID_TAG_KEY);
    final LabelId secondId = CassandraLabelId.fromLong(LARGE_ID_METRIC);
    assertFalse(firstId.equals(secondId));
  }

  @Test
  public void testGenerateIdHashes() {
    final String name = "testString";
    final long id = Hashing.murmur3_128().hashString(name, CassandraConst.CHARSET).asLong();
    final long typeId = makeIdTypeSpecific(id, METRIC);

    assertEquals(typeId, CassandraLabelId.generateId(name, METRIC));
  }

  @Test
  public void testMakeIdTypeSpecificMasksMetric() throws Exception {
    assertEquals(0b00, makeIdTypeSpecific(SMALL_ID, METRIC));
    assertEquals(LARGE_ID_METRIC, makeIdTypeSpecific(LARGE_ID, METRIC));
  }

  @Test
  public void testMakeIdTypeSpecificMasksTagKey() throws Exception {
    assertEquals(0b10, makeIdTypeSpecific(SMALL_ID, TAGK));
    assertEquals(LARGE_ID_TAG_KEY, makeIdTypeSpecific(LARGE_ID, TAGK));
  }

  @Test
  public void testMakeIdTypeSpecificMasksTagValue() throws Exception {
    assertEquals(0b01, makeIdTypeSpecific(SMALL_ID, TAGV));
    assertEquals(LARGE_ID_TAG_VALUE, makeIdTypeSpecific(LARGE_ID, TAGV));
  }

  @Test(expected = AssertionError.class)
  public void testTypeBadId() throws Exception {
    CassandraLabelId.fromLong(LARGE_ID).type();
  }

  @Test
  public void testTypeMetric() throws Exception {
    final CassandraLabelId labelId = CassandraLabelId.fromLong(LARGE_ID_METRIC);
    assertEquals(METRIC, labelId.type());
  }

  @Test
  public void testTypeTagKey() throws Exception {
    final CassandraLabelId labelId = CassandraLabelId.fromLong(LARGE_ID_TAG_KEY);
    assertEquals(TAGK, labelId.type());
  }

  @Test
  public void testTypeTagValue() throws Exception {
    final CassandraLabelId labelId = CassandraLabelId.fromLong(LARGE_ID_TAG_VALUE);
    assertEquals(TAGV, labelId.type());
  }
}
