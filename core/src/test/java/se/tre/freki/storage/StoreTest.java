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
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public abstract class StoreTest<K extends Store> {


  private static final String NAME = "oldName";
  private static final String NEW = "new";
  private static final String MISSING = "missing";
  private static final LabelType TYPE = LabelType.TAGK;

  private static LabelId NAME_ID;

  protected K store;

  @Before
  public void setUp() throws Exception {
    store = buildStore();
    NAME_ID = store.createLabel(NAME, TYPE).get();
  }

  protected abstract K buildStore();

  /**
   * Get a label id that the store perceives as not being created. Since we have no regulations on a
   * label id can look like there is no general way to create one, thereby this method.
   */
  protected abstract LabelId missingLabelId();

  @Test
  public void testAllocateLabelExistingException() throws Exception {
    try {
      store.createLabel(NAME, TYPE).get();
      fail("The second allocation with the same name should have thrown an exception");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof LabelException);
    }
  }

  @Test
  public void testAllocateLabelNotExisting() throws Exception {
    final LabelId label = store.createLabel(NEW, TYPE).get();
    final Optional<LabelId> fetchedLabel = store.getId(NEW, TYPE).get();
    assertEquals(label, fetchedLabel.get());
  }

  @Test
  public void testGetIdExisting() throws Exception {
    final Optional<LabelId> fetchedLabel = store.getId(NAME, TYPE).get();
    assertEquals(NAME_ID, fetchedLabel.get());
  }

  @Test
  public void testGetIdMissingAbsent() throws Exception {
    final Optional<LabelId> missing = store.getId(MISSING, TYPE).get();
    assertFalse(missing.isPresent());
  }

  @Test
  public void testGetNameExisting() throws Exception {
    final Optional<String> fetchedLabel = store.getName(NAME_ID, TYPE).get();
    assertEquals(NAME, fetchedLabel.get());
  }

  @Test
  public void testGetNameMissingAbsent() throws Exception {
    final Optional<String> missing = store.getName(missingLabelId(), TYPE).get();
    assertFalse(missing.isPresent());
  }

  @Test
  public void testRenameIdFoundOnNewName() throws Exception {
    store.renameLabel(NEW, NAME_ID, TYPE).get().get();
    final LabelId newNameId = store.getId(NEW, TYPE).get().get();
    assertEquals(NAME_ID, newNameId);
  }

  @Test
  public void testRenameIdNotFoundOnOldName() throws Exception {
    store.renameLabel(NEW, NAME_ID, TYPE).get().get();
    assertFalse(store.getId(NAME, TYPE).get().isPresent());
  }
}
