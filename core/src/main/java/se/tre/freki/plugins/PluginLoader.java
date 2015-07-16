
package se.tre.freki.plugins;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * A simple plugin architecture based around Javas {@link ServiceLoader}.
 *
 * <p>Due to the reliance on {@link ServiceLoader} all plugins must have a parameter less
 * constructor. Plugins that require arguments can be implemented by loading a "descriptor" through
 * {@link #pluginWithName(String)} which defines the interface for creating the plugin like in
 * {@link se.tre.freki.search.SearchPluginDescriptor}.
 */
public final class PluginLoader<T> {
  private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

  private final Class<T> pluginType;
  private final Iterable<T> plugins;

  PluginLoader(final Class<T> pluginType, final Iterable<T> plugins) {
    this.pluginType = checkNotNull(pluginType);
    this.plugins = checkNotNull(plugins);
  }

  /**
   * Create a new plugin loader that will instantiate plugins of the given type.
   *
   * @param pluginType The class object of the type of plugins this plugin loader will load
   * @param <T> The type of plugins this plugin loader will load
   * @return An instantiated plugin loader
   */
  public static <T> PluginLoader<T> forType(final Class<T> pluginType) {
    return new PluginLoader<>(pluginType, loadPlugins(pluginType));
  }

  /**
   * Search the class path and load all and create an instance of every found class of the given
   * type.
   *
   * @param type The class type to search for
   * @return A list of instantiated objects of the given type.
   */
  private static <T> Iterable<T> loadPlugins(final Class<T> type) {
    ServiceLoader<T> serviceLoader = ServiceLoader.load(type);
    final ImmutableList<T> plugins = ImmutableList.copyOf(serviceLoader);
    LOG.info("Found {} plugins of type {}", plugins.size(), type);
    return plugins;
  }

  /**
   * Search the class path and return an instantiated object of the given name and type. Note that
   * all plugins of the given type will be instantiated but only the one with the provided name will
   * be returned.
   *
   * @return The instantiated plugin of the given type
   * @throws IllegalArgumentException if the name was missing or empty, the type was missing or no
   * plugin with that name was found
   */
  public T pluginWithName(final String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name),
        "Missing plugin name");

    for (final T plugin : plugins) {
      if (plugin.getClass().getName().equals(name)) {
        return plugin;
      }
    }

    throw new IllegalStateException("Unable to find plugin with name "
                                    + name + " of type " + pluginType);
  }
}
