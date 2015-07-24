package se.tre.freki.web.jackson;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.LabelMeta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

abstract class LabelMetaMixIn {
  @JsonCreator
  static LabelMeta create(@JsonProperty("identifier") final LabelId identifier,
                          @JsonProperty("type") final LabelType type,
                          @JsonProperty("name") final String name,
                          @JsonProperty("description") final String description,
                          @JsonProperty("created") final long created) {
    return LabelMeta.create(identifier, type, name, description, created);
  }

  @JsonProperty
  abstract LabelId identifier();

  /** The type of UID this metadata represents. */
  @JsonProperty
  @JsonDeserialize(using = UniqueIdTypeDeserializer.class)
  abstract LabelType type();

  @JsonProperty
  abstract String name();

  @JsonProperty
  abstract String description();

  @JsonProperty
  abstract long created();
}
