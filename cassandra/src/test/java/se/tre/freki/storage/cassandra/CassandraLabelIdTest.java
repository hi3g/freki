package se.tre.freki.storage.cassandra;

import static org.junit.Assert.*;

import se.tre.freki.labels.LabelType;

import com.google.common.hash.Hashing;
import org.junit.Test;

public class CassandraLabelIdTest {
  @Test
  public void makeLabelSpecificId() {
    assertEquals(0, CassandraLabelId.makeLabelSpecificId(0, LabelType.METRIC));
    assertEquals(0, CassandraLabelId.makeLabelSpecificId(3, LabelType.METRIC));
    assertEquals(4, CassandraLabelId.makeLabelSpecificId(4, LabelType.METRIC));

    assertEquals(1, CassandraLabelId.makeLabelSpecificId(0, LabelType.TAGV));
    assertEquals(1, CassandraLabelId.makeLabelSpecificId(3, LabelType.TAGV));
    assertEquals(5, CassandraLabelId.makeLabelSpecificId(4, LabelType.TAGV));

    assertEquals(2, CassandraLabelId.makeLabelSpecificId(0, LabelType.TAGK));
    assertEquals(2, CassandraLabelId.makeLabelSpecificId(3, LabelType.TAGK));
    assertEquals(6, CassandraLabelId.makeLabelSpecificId(4, LabelType.TAGK));
  }

  @Test
  public void generateId() {

    final String name = "testString";

    long id = Hashing.murmur3_128().hashString(name, CassandraConst.CHARSET).asLong();
    id = id >> 2;
    id = id << 2;

    assertEquals(id, CassandraLabelId.generateId(name, LabelType.METRIC));
  }
}