package se.tre.freki.web.jackson;

import se.tre.freki.labels.LabelId;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

public class LabelIdJsonKeyDeserializer extends KeyDeserializer {
  private final LabelId.LabelIdDeserializer deserializer;

  public LabelIdJsonKeyDeserializer(final LabelId.LabelIdDeserializer deserializer) {
    this.deserializer = deserializer;
  }

  @Override
  public Object deserializeKey(final String key, final DeserializationContext ctxt)
      throws IOException {
    return deserializer.deserialize(key);
  }
}
