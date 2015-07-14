package se.tre.freki.search;

import se.tre.freki.storage.Store;

import com.google.auto.service.AutoService;
import com.typesafe.config.Config;

@AutoService(SearchPluginDescriptor.class)
public class DefaultSearchPluginDescriptor extends SearchPluginDescriptor {
  @Override
  public SearchPlugin create(final Config config, final Store store) throws Exception {
    return new DefaultSearchPlugin();
  }
}
