package se.tre.freki.web.resources;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import se.tre.freki.core.DataPointsClient;
import se.tre.freki.query.DataPoint;
import se.tre.freki.query.DecoratedTimeSeriesId;
import se.tre.freki.query.QueryException;
import se.tre.freki.utils.AsyncIterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A resource that accepts post requests whose body contains a JSON array of datapoints.
 */
public final class QueryResource extends Resource {
  private static final Logger LOG = LoggerFactory.getLogger(QueryResource.class);

  private final DataPointsClient datapointsClient;
  private final ObjectMapper objectMapper;

  public QueryResource(final DataPointsClient datapointsClient,
                       final ObjectMapper objectMapper) {
    this.datapointsClient = checkNotNull(datapointsClient);
    this.objectMapper = checkNotNull(objectMapper);
  }

  @Override
  protected FullHttpResponse doGet(final FullHttpRequest request) {
    try {
      final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
      final Map<String, List<String>> parameters = decoder.parameters();
      final List<String> queries = parameters.get("q");

      if (queries == null || queries.isEmpty()) {
        LOG.info("Received query request with missing query parameter");
        return response(BAD_REQUEST);
      }

      final String query = queries.get(0);

      if (Strings.isNullOrEmpty(query)) {
        LOG.info("Received query request with empty query parameter");
        return response(BAD_REQUEST);
      }

      final Map<DecoratedTimeSeriesId, AsyncIterator<? extends DataPoint>> dataPoints =
          datapointsClient.query(query).get();

      final ByteArrayOutputStream out = new ByteArrayOutputStream();

      final JsonFactory jsonFactory = objectMapper.getFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createGenerator(out);

      jsonGenerator.writeStartObject();

      for (final Map.Entry<DecoratedTimeSeriesId, AsyncIterator<? extends DataPoint>> timeSeries :
          dataPoints.entrySet()) {
        jsonGenerator.writeArrayFieldStart(timeSeries.getKey().toString());

        final Iterator<? extends DataPoint> timeSerieDataPoints = timeSeries.getValue();

        while (timeSerieDataPoints.hasNext()) {
          final DataPoint next = timeSerieDataPoints.next();
          jsonGenerator.writeStartArray(2);

          if (next instanceof DataPoint.LongDataPoint) {
            jsonGenerator.writeNumber(next.longValue());
          } else if (next instanceof DataPoint.FloatDataPoint) {
            jsonGenerator.writeNumber(next.floatValue());
          } else if (next instanceof DataPoint.DoubleDataPoint) {
            jsonGenerator.writeNumber(next.doubleValue());
          } else {
            throw new AssertionError("Almost wrote a data point with an impossible data type:"
                                     + next);
          }

          jsonGenerator.writeNumber(next.timestamp());
          jsonGenerator.writeEndArray();
        }

        jsonGenerator.writeEndArray();
      }

      jsonGenerator.writeEndObject();
      jsonGenerator.close();

      return response(OK, Unpooled.wrappedBuffer(out.toByteArray()));
    } catch (QueryException e) {
      LOG.warn("Encountered an exception while executing query", e);
      return response(BAD_REQUEST);
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while executing query", e);
      return response(INTERNAL_SERVER_ERROR);
    } catch (ExecutionException e) {
      LOG.warn("Encountered an exception while executing query", e);
      return response(INTERNAL_SERVER_ERROR);
    } catch (IOException e) {
      LOG.warn("Encountered an exception while writing response", e);
      return response(INTERNAL_SERVER_ERROR);
    }
  }
}
