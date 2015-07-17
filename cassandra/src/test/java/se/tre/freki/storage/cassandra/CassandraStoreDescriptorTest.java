package se.tre.freki.storage.cassandra;

import se.tre.freki.storage.StoreDescriptor;
import se.tre.freki.storage.StoreDescriptorTest;
import se.tre.freki.storage.StoreModule;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.junit.Test;

import javax.annotation.Nullable;

public class CassandraStoreDescriptorTest extends StoreDescriptorTest {
  @Override
  protected StoreDescriptor buildStoreDescriptor() {
    return new CassandraStoreDescriptor();
  }

  @Test
  public void testStoreModuleFindsCassandraStore() {
    final StoreModule storeModule = new StoreModule();
    final Iterable<StoreDescriptor> storeDescriptors = storeModule.provideStoreDescriptors();

    Iterables.find(storeDescriptors, new Predicate<StoreDescriptor>() {
      @Override
      public boolean apply(@Nullable final StoreDescriptor storeDescriptor) {
        return storeDescriptor instanceof CassandraStoreDescriptor;
      }
    });
  }
}
