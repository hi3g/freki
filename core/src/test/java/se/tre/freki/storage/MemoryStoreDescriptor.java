package se.tre.freki.storage;

import se.tre.freki.labels.LabelId;

import com.google.auto.service.AutoService;
import com.typesafe.config.Config;

import javax.annotation.Nonnull;

@AutoService(StoreDescriptor.class)
public class MemoryStoreDescriptor extends StoreDescriptor {
  @Override
  public Store createStore(final Config config) {
    return new MemoryStore();
  }

  @Nonnull
  @Override
  public LabelId.LabelIdSerializer<MemoryLabelId> labelIdSerializer() {
    return new MemoryLabelId.MemoryLabelIdSerializer();
  }

  @Nonnull
  @Override
  public LabelId.LabelIdDeserializer labelIdDeserializer() {
    return new MemoryLabelId.MemoryLabelIdDeserializer();
  }
}
