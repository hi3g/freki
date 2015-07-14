package se.tre.freki.storage;

import com.typesafe.config.Config;

public abstract class StoreTest<K extends Store> {
  protected K store;
  protected Config config;
}
