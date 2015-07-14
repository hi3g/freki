package se.tre.freki.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.labels.LabelId;
import se.tre.freki.utils.InvalidConfigException;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class StoreModuleTest {
  /**
   * A constant that describes the number of stores that the core project comes with and thus how
   * many store descriptors that the {@link java.util .ServiceLoader} should be able to find.
   *
   * <p>The only store provided by the core project is the {@link MemoryStore} which explains the
   * number the one.
   */
  private static final int NUM_STORES = 1;

  @Inject Config config;

  private Iterable<StoreDescriptor> storeDescriptors;
  private StoreModule supplier;

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);

    storeDescriptors = ImmutableSet.<StoreDescriptor>of(new TestStoreDescriptor());
    supplier = new StoreModule();
  }

  @Test(expected = InvalidConfigException.class)
  public void testGetEmptyConfig() throws Exception {
    config = config.withValue("freki.storage.adapter",
        ConfigValueFactory.fromAnyRef(""));
    supplier.provideStoreDescriptor(config, storeDescriptors);
  }

  @Test
  public void testGetMatchingStore() throws Exception {
    config = config.withValue("freki.storage.adapter",
        ConfigValueFactory.fromAnyRef("se.tre.freki.storage.StoreModuleTest.TestStoreDescriptor"));
    StoreDescriptor storeDescriptor =
        supplier.provideStoreDescriptor(config, storeDescriptors);
    assertTrue(storeDescriptor instanceof TestStoreDescriptor);
  }

  @Test(expected = InvalidConfigException.class)
  public void testGetNoMatchingStore() throws Exception {
    config = config.withValue("freki.storage.adapter",
        ConfigValueFactory.fromAnyRef("FooBar4711"));
    supplier.provideStoreDescriptor(config, storeDescriptors);
  }

  @Test
  public void testNumberOfFoundStoreDescriptors() {
    Iterable<StoreDescriptor> storeDescriptors = supplier.provideStoreDescriptors();
    assertEquals(NUM_STORES, Iterables.size(storeDescriptors));
  }

  private static class TestStoreDescriptor extends StoreDescriptor {
    @Override
    public Store createStore(final Config config, final MetricRegistry metrics) {
      return mock(Store.class);
    }

    @Nonnull
    @Override
    public LabelId.LabelIdSerializer labelIdSerializer() {
      return mock(LabelId.LabelIdSerializer.class);
    }

    @Nonnull
    @Override
    public LabelId.LabelIdDeserializer labelIdDeserializer() {
      return mock(LabelId.LabelIdDeserializer.class);
    }
  }
}
