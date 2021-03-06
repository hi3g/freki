package se.tre.freki.labelmanager;

import se.tre.freki.core.ConfigModule;
import se.tre.freki.core.CoreModule;
import se.tre.freki.plugins.PluginsModule;
import se.tre.freki.storage.Store;
import se.tre.freki.storage.StoreModule;

import dagger.Component;

import javax.inject.Singleton;

@Component(
    modules = {
        ConfigModule.class,
        CoreModule.class,
        PluginsModule.class,
        StoreModule.class
    })
@Singleton
public interface CreateLabelComponent {
  CreateLabel createLabel();

  Store store();
}
