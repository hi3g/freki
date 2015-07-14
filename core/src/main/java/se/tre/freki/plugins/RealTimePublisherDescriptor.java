package se.tre.freki.plugins;

import com.typesafe.config.Config;

public abstract class RealTimePublisherDescriptor {
  /**
   * Called to create an instance of the publisher plugin that this descriptor represents. The
   * publisher plugin is expected to be ready to receive data when this method returns without
   * throwing an exception.
   *
   * @param config A config that the search plugin can read from
   * @return A newly instantiated and ready publisher plugin
   * @throws Exception if anything during the plugin initialization failed
   */
  public abstract RealTimePublisher create(Config config) throws Exception;
}
