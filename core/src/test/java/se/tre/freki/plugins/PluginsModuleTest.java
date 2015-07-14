package se.tre.freki.plugins;

import static org.junit.Assert.assertTrue;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.TestComponent;
import se.tre.freki.core.ConfigModule;
import se.tre.freki.search.DefaultSearchPlugin;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

public class PluginsModuleTest {
  private TestComponent component;

  @Before
  public void setUp() throws Exception {
    component = DaggerTestComponent.create();
  }

  @Test(expected = IllegalStateException.class)
  public void testProvideRealTimePublisherThrowsExceptionWrongConfig() {
    final TestComponent component = DaggerTestComponent.builder()
        .configModule(ConfigModule.defaultWithOverrides(
            ImmutableMap.of("freki.publisher.plugin", "doesNotExist")))
        .build();

    component.realTimePublisher();
  }

  @Test
  public void testProvideRealtimePublisherDefaultPublisher() {
    assertTrue(component.realTimePublisher() instanceof DefaultRealtimePublisher);
  }

  @Test
  public void testProvideSearchPluginDefaultPlugin() {
    assertTrue(component.searchPlugin() instanceof DefaultSearchPlugin);
  }

  @Test(expected = IllegalStateException.class)
  public void testProvideSearchPluginThrowsExceptionWrongConfig() {
    final TestComponent component = DaggerTestComponent.builder()
        .configModule(ConfigModule.defaultWithOverrides(
            ImmutableMap.of("freki.search.plugin", "doesNotExist")))
        .build();

    component.searchPlugin();
  }
}
