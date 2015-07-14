package se.tre.freki.web;

import se.tre.freki.core.ConfigModule;
import se.tre.freki.core.CoreModule;
import se.tre.freki.core.DataPointsClient;
import se.tre.freki.core.LabelClient;
import se.tre.freki.plugins.PluginsModule;
import se.tre.freki.storage.StoreDescriptor;
import se.tre.freki.storage.StoreModule;
import se.tre.freki.web.jackson.JacksonModule;
import se.tre.freki.web.resources.DatapointsResource;
import se.tre.freki.web.resources.LabelResource;
import se.tre.freki.web.resources.MetricsResource;
import se.tre.freki.web.resources.NotFoundResource;
import se.tre.freki.web.resources.Resource;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import io.netty.handler.codec.http.cors.CorsConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

/**
 * A Dagger module that is capable of creating objects relevant to the HTTP server.
 */
@Module(
    includes = {
        ConfigModule.class,
        CoreModule.class,
        PluginsModule.class,
        StoreModule.class
    })
public class HttpModule {
  @Provides
  @Singleton
  ObjectMapper provideObjectMapper(final StoreDescriptor storeDescriptor) {
    return new ObjectMapper()
        .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))
        .registerModule(new JacksonModule(storeDescriptor.labelIdSerializer(),
            storeDescriptor.labelIdDeserializer()))
        .registerModule(new GuavaModule())
        .enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
  }

  @Provides
  @Singleton
  HttpServerInitializer provideHttpServerInitializer(final Config config,
                                                     final DataPointsClient dataPointsClient,
                                                     final ObjectMapper objectMapper,
                                                     final MetricRegistry metricRegistry,
                                                     final LabelClient labelClient) {
    final Resource datapointsResource = new DatapointsResource(dataPointsClient, objectMapper);
    final Resource metricResource = new MetricsResource(objectMapper, metricRegistry);
    final Resource idResource = new LabelResource(labelClient, objectMapper);

    final ImmutableMap<String, Resource> resources = ImmutableMap.of(
        "datapoints", datapointsResource,
        "admin/metrics", metricResource,
        "labels", idResource);
    final Resource defaultResource = new NotFoundResource();

    // http://lmgtfy.com/?q=facepalm
    final List<String> corsHeaders = config.getStringList("freki.web.cors.request.headers");
    final String[] corsHeadersArray = corsHeaders.toArray(new String[corsHeaders.size()]);

    final List<String> corsDomains = config.getStringList("freki.web.cors_domains");
    final String[] corsDomainsArray = corsDomains.toArray(new String[corsDomains.size()]);

    final CorsConfig corsConfig = CorsConfig
        .withOrigins(corsDomainsArray)
        .allowedRequestHeaders(corsHeadersArray)
        .build();

    final int maxContentLength = config.getInt("freki.web.max_content_length");

    return new HttpServerInitializer(resources, defaultResource, corsConfig, maxContentLength);
  }
}
