package se.tre.freki.web.jackson;

import se.tre.freki.labels.LabelId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class LabelIdJsonSerializer extends JsonSerializer<LabelId> {
  private final LabelId.LabelIdSerializer serializer;

  LabelIdJsonSerializer(LabelId.LabelIdSerializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public void serialize(final LabelId value,
                        final JsonGenerator gen,
                        final SerializerProvider serializers)
      throws IOException, JsonProcessingException {
    gen.writeString(serializer.serialize(value));
  }

  @Override
  public Class<LabelId> handledType() {
    return LabelId.class;
  }
}
