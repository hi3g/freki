package se.tre.freki.storage;

import se.tre.freki.labels.LabelId;

import com.codahale.metrics.MetricRegistry;
import com.typesafe.config.Config;

import javax.annotation.Nonnull;

public abstract class StoreDescriptor {
  public abstract Store createStore(Config config, MetricRegistry metrics);

  /**
   * Get an object that is capable of serializing the {@link LabelId} implementation used by this
   * store.
   *
   * @return A non-null serializer.
   */
  @Nonnull
  public abstract LabelId.LabelIdSerializer labelIdSerializer();

  /**
   * Get an object that is capable of deserializing the {@link LabelId} implementation used by this
   * store.
   *
   * @return A non-null deserializer.
   */
  @Nonnull
  public abstract LabelId.LabelIdDeserializer labelIdDeserializer();
}
