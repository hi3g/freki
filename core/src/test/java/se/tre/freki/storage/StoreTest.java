package se.tre.freki.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import se.tre.freki.labels.LabelException;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public abstract class StoreTest<K extends Store> {
  protected K store;

  @Before
  public void setUp() throws Exception {
    store = buildStore();
  }

  protected abstract K buildStore();

  /**
   * Get a label id that the store perceives as not being created. Since we have no regulations on a
   * label id can look like there is no general way to create one, thereby this method.
   */
  protected abstract LabelId missingLabelId();

  @Test
  public void testAllocateLabelExistingException() throws Exception {
    store.createLabel("newname", LabelType.TAGK).get();

    try {
      store.createLabel("newname", LabelType.TAGK).get();
      fail("The second allocation with the same name should have thrown an exception");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof LabelException);
    }
  }

  @Test
  public void testAllocateLabelNotExisting() throws Exception {
    final LabelId label = store.createLabel("newname", LabelType.TAGK).get();
    final Optional<LabelId> fetchedLabel = store.getId("newname", LabelType.TAGK).get();
    assertEquals(label, fetchedLabel.get());
  }

  @Test
  public void testGetIdExisting() throws Exception {
    final LabelId newLabel = store.createLabel("newname", LabelType.TAGV).get();
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
    final LabelId newLabel = store.createLabel("newname", LabelType.TAGV).get();
    final Optional<String> fetchedLabel = store.getName(newLabel, LabelType.TAGV).get();
    assertEquals("newname", fetchedLabel.get());
  }

  @Test
  public void testGetNameMissingAbsent() throws Exception {
    final Optional<String> missing = store.getName(missingLabelId(), LabelType.TAGV).get();
    assertFalse(missing.isPresent());
  }

  @Test
  @Ignore
  public void testRenameIdFoundOnNewName() throws Exception {
    final LabelId id = store.createLabel("name", LabelType.TAGK).get();
    store.renameLabel("newname", id, LabelType.TAGK).get();
    final LabelId newNameId = store.getId("newname", LabelType.TAGK).get().get();
    assertEquals(id, newNameId);
  }

  @Test
  @Ignore
  public void testRenameIdNotFoundOnOldName() throws Exception {
    final LabelId id = store.createLabel("name", LabelType.TAGK).get();
    store.renameLabel("newname", id, LabelType.TAGK).get();
    assertFalse(store.getId("name", LabelType.TAGK).get().isPresent());
  }

  @Test
  @Ignore
  public void testRenameIdNotFound() throws Exception {
    store.renameLabel("name", missingLabelId(), LabelType.TAGK).get();
  }
}
