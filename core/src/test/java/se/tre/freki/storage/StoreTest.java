package se.tre.freki.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.google.common.base.Optional;
import org.junit.Test;

public abstract class StoreTest<K extends Store> {
  protected K store;

  /**
   * Get a label id that the store perceives as not being created. Since we have no regulations on a
   * label id can look like there is no general way to create one, thereby this method.
   */
  protected abstract LabelId missingLabelId();

  @Test
  public void testGetIdExisting() throws Exception {
    final LabelId newLabel = store.allocateLabel("newname", LabelType.TAGV).get();
    final Optional<LabelId> fetchedLabel = store.getId("newname", LabelType.TAGV).get();
    assertEquals(newLabel, fetchedLabel.get());
  }

  @Test
  public void testGetIdMissingAbsent() throws Exception {
    final Optional<LabelId> missing = store.getId("missing", LabelType.TAGV).get();
    assertFalse(missing.isPresent());
  }

  @Test
  public void testGetNameExisting() throws Exception {
    final LabelId newLabel = store.allocateLabel("newname", LabelType.TAGV).get();
    final Optional<String> fetchedLabel = store.getName(newLabel, LabelType.TAGV).get();
    assertEquals("newname", fetchedLabel.get());
  }

  @Test
  public void testGetNameMissingAbsent() throws Exception {
    final Optional<String> missing = store.getName(missingLabelId(), LabelType.TAGV).get();
    assertFalse(missing.isPresent());
  }
}
