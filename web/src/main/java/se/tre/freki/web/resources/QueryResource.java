package se.tre.freki.web.resources;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import se.tre.freki.core.DataPointsClient;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.query.DataPoint;
import se.tre.freki.query.QueryException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
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

      final String query = parameters.get("q").get(0);
      final Map<TimeSeriesId, Iterator<? extends DataPoint>> dataPoints =
          datapointsClient.query(query).get();

      final ByteArrayOutputStream out = new ByteArrayOutputStream();

      final JsonFactory jsonFactory = objectMapper.getFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createGenerator(out);

      jsonGenerator.writeStartObject();

      for (final Map.Entry<TimeSeriesId, Iterator<? extends DataPoint>> timeSeries : dataPoints.entrySet()) {
        jsonGenerator.writeArrayFieldStart(timeSeries.getKey().toString());

        final Iterator<? extends DataPoint> dps = timeSeries.getValue();

        while (dps.hasNext()) {
          final DataPoint next = dps.next();
          jsonGenerator.writeStartArray(2);

          if (next instanceof DataPoint.LongDataPoint) {
            jsonGenerator.writeNumber(((DataPoint.LongDataPoint) next).value());
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
      return response(BAD_REQUEST);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return response(BAD_REQUEST);
    } catch (ExecutionException e) {
      e.printStackTrace();
      return response(BAD_REQUEST);
    } catch (IOException e) {
      e.printStackTrace();
      return response(BAD_REQUEST);
    }
  }
}
