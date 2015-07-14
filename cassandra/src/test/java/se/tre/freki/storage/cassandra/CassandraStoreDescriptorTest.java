package se.tre.freki.storage.cassandra;

import se.tre.freki.storage.StoreDescriptorTest;

import org.junit.Before;

public class CassandraStoreDescriptorTest extends StoreDescriptorTest {
  @Before
  public void setUp() throws Exception {
    storeDescriptor = new CassandraStoreDescriptor();
  }
}
