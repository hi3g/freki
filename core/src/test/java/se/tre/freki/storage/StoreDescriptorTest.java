package se.tre.freki.storage;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Generic test for {@link StoreDescriptor}s. Extend this class for tests of the implementations of
 * the {@link StoreDescriptor} class and set the {@link #storeDescriptor} variable in the before
 * block.
 */
public abstract class StoreDescriptorTest {
  protected StoreDescriptor storeDescriptor;

  @Before
  public void setUp() throws Exception {
    storeDescriptor = buildStoreDescriptor();
  }

  protected abstract StoreDescriptor buildStoreDescriptor();

  @Test
  public void testLabelIdDeserializerNotNull() {
    assertNotNull(storeDescriptor.labelIdDeserializer());
  }

  @Test
  public void testLabelIdSerializerNotNull() {
    assertNotNull(storeDescriptor.labelIdDeserializer());
  }
}
