package se.tre.freki.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import se.tre.freki.search.DefaultSearchPlugin;
import se.tre.freki.search.SearchPlugin;

import org.junit.Test;

import java.util.List;

public final class PluginLoaderTest {
  @Test
  public void loadPluginWithName() {
    final SearchPlugin plugin = PluginLoader.loadPluginWithName(
        "se.tre.freki.search.DefaultSearchPlugin", SearchPlugin.class);
    assertTrue(plugin instanceof DefaultSearchPlugin);
  }

  @Test(expected = IllegalStateException.class)
  public void loadPluginWithNameNotFound() {
    PluginLoader.loadPluginWithName("NoImplementation", SearchPlugin.class);
  }

  @Test
  public void loadPlugins() throws Exception {
    List<SearchPlugin> plugins = PluginLoader.loadPlugins(SearchPlugin.class);
    assertEquals(1, plugins.size());
  }

  @Test
  public void loadPluginsNotFound() {
    List<PluginLoaderTest> plugins = PluginLoader.loadPlugins(PluginLoaderTest.class);
    assertEquals(0, plugins.size());
  }
}
