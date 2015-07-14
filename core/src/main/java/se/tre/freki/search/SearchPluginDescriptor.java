package se.tre.freki.search;

import se.tre.freki.storage.Store;

import com.typesafe.config.Config;

public abstract class SearchPluginDescriptor {
  /**
   * Called to create an instance of the search plugin that this descriptor represents. The search
   * plugin is expected to be ready to receive data when this method returns without throwing an
   * exception.
   *
   * @param config A config that the search plugin can read from
   * @param store The store from which the search plugin can read from
   * @return A newly instantiated and ready search plugin
   * @throws Exception if anything during the plugin initialization failed
   */
  public abstract SearchPlugin create(final Config config, final Store store) throws Exception;
}
