package se.tre.freki.web.jackson;

import se.tre.freki.labels.LabelId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class LabelIdJsonDeserializer extends JsonDeserializer<LabelId> {
  private final LabelId.LabelIdDeserializer deserializer;

  LabelIdJsonDeserializer(LabelId.LabelIdDeserializer deserializer) {
    this.deserializer = deserializer;
  }

  @Override
  public LabelId deserialize(final JsonParser parser, final DeserializationContext ctxt)
      throws IOException {
    return deserializer.deserialize(parser.getValueAsString());
  }

  @Override
  public Class<?> handledType() {
    return LabelId.class;
  }
}
