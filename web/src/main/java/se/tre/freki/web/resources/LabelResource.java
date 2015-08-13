package se.tre.freki.web.resources;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.UNPROCESSABLE_ENTITY;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A Resource that accepts POST requests whose body contains a JSON containing a ID.
 */
public final class LabelResource extends Resource {
  private static final Logger LOG = LoggerFactory.getLogger(LabelResource.class);

  private final LabelClient labelClient;
  private final ObjectMapper objectMapper;

  public LabelResource(final LabelClient labelClient, final ObjectMapper objectMapper) {
    this.labelClient = checkNotNull(labelClient);
    this.objectMapper = checkNotNull(objectMapper);
  }

  @Override
  protected FullHttpResponse doPost(FullHttpRequest req) {
    try {
      final JsonNode rootNode = objectMapper.readTree(new ByteBufInputStream(req.content()));

      final String name = rootNode.get("name").asText();
      final LabelType type = LabelType.fromValue(rootNode.get("type").asText());

      Futures.addCallback(labelClient.createId(type, name), new FutureCallback<LabelId>() {
        @Override
        public void onSuccess(final LabelId id) {
          LOG.info("Created label with name {} and type {} with ID {}", name, type, id);
        }

        @Override
        public void onFailure(final Throwable throwable) {
          LOG.error("Unable to create label with name {} and type {}", name, type, throwable);
        }
      });

      return response(ACCEPTED);
    } catch (JsonProcessingException e) {
      LOG.info("Malformed JSON while adding new label", e);
      return response(UNPROCESSABLE_ENTITY);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
