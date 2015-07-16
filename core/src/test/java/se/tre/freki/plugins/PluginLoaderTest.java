package se.tre.freki.plugins;

import static org.junit.Assert.assertTrue;

import se.tre.freki.search.DefaultSearchPlugin;
import se.tre.freki.search.SearchPlugin;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

public final class PluginLoaderTest {
  private PluginLoader<SearchPlugin> pluginLoader;

  @Before
  public void setUp() throws Exception {
    final Iterable<SearchPlugin> plugins = ImmutableSet.<SearchPlugin>of(
        new DefaultSearchPlugin());
    pluginLoader = new PluginLoader<>(SearchPlugin.class, plugins);
  }

  @Test
  public void loadPluginWithName() {
    final SearchPlugin plugin = pluginLoader.pluginWithName(
        "se.tre.freki.search.DefaultSearchPlugin");
    assertTrue(plugin instanceof DefaultSearchPlugin);
  }

  @Test(expected = IllegalStateException.class)
  public void loadPluginWithNameNotFound() {
    pluginLoader.pluginWithName("NoImplementation");
  }
}
