package se.tre.freki.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.addCallback;

import se.tre.freki.labels.Labels;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.plugins.PluginError;
import se.tre.freki.plugins.RealTimePublisher;
import se.tre.freki.query.TimeSeriesResultSet;
import se.tre.freki.stats.Metrics;
import se.tre.freki.stats.StopTimerCallback;
import se.tre.freki.storage.Store;
import se.tre.freki.time.Timestamps;
import se.tre.freki.utils.InvalidConfigException;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import com.google.common.primitives.SignedBytes;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.typesafe.config.Config;

import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataPointsClient {
  private final Store store;
  private final LabelClient labelClient;
  private final RealTimePublisher publisher;

  private final Timer addDataPointTimer;
  private final byte maxTags;

  /**
   * Create a new instance using the given non-null arguments to configure itself.
   */
  @Inject
  public DataPointsClient(final Store store,
                          final LabelClient labelClient,
                          final RealTimePublisher realTimePublisher,
                          final MetricRegistry metricRegistry,
                          final Config config) {
    this.store = checkNotNull(store);
    this.labelClient = checkNotNull(labelClient);
    this.publisher = checkNotNull(realTimePublisher);

    this.addDataPointTimer = metricRegistry.timer(Metrics.name("add_data_point"));

    // The config library unfortunately doesn't have any API to get any smaller primitive type than
    // ints so we have to do a little dance to make sure the value is not too extreme since it can
    // have a significant impact on query performance.
    final int configMaxTags = config.getInt("freki.core.max_tags");

    if (configMaxTags > Byte.MAX_VALUE) {
      throw new InvalidConfigException(config.getValue("freki.core.max_tags"),
          "The number of maximum allowed tags must not be larger than " + Byte.MAX_VALUE);
    }

    if (configMaxTags < 1) {
      throw new InvalidConfigException(config.getValue("freki.core.max_tags"),
          "At least one tag must be allowed");
    }

    this.maxTags = SignedBytes.checkedCast(configMaxTags);
  }

  /**
   * Makes sure that the input arguments are non-null, non-empty and does not contain any illegal
   * characters. It also makes sure that the number of tags does not exceed the configured limit.
   *
   * @param metric The metric to validate
   * @param tags The tags to validate
   * @throws IllegalArgumentException if any validation fails
   */
  private void checkMetricAndTags(final String metric, final Map<String, String> tags) {
    checkArgument(!Strings.isNullOrEmpty(metric), "Missing metric name", metric, tags);
    checkArgument(!tags.isEmpty(), "At least one tag is required", metric, tags);
    checkArgument(tags.size() <= maxTags,
        "No more than %s tags are allowed but there are %s",
        maxTags, tags.size(), metric, tags);

    Labels.checkLabelName("metric name", metric);
    for (final Map.Entry<String, String> tag : tags.entrySet()) {
      Labels.checkLabelName("tag name", tag.getKey());
      Labels.checkLabelName("tag value", tag.getValue());
    }
  }

  /**
   * Add a floating point data point at the provided timestamp that will belong to the time series
   * behind the given metric and tags. See {@link #checkMetricAndTags(String, Map)} for the contract
   * that the metric and tags must fulfill and see {@link Timestamps#checkTimestamp(long)} for the
   * contract that the timestamp must fulfill.
   *
   * @return A future that indicates the completion of the request or an error.
   */
  public ListenableFuture<Void> addPoint(final String metric,
                                         final long timestamp,
                                         final float value,
                                         final Map<String, String> tags) {
    Timestamps.checkTimestamp(timestamp);
    checkMetricAndTags(metric, tags);

    class AddPointFunction implements AsyncFunction<TimeSeriesId, Void> {
      @Override
      public ListenableFuture<Void> apply(final TimeSeriesId timeSeriesId) {
        ListenableFuture<Void> result = store.addPoint(timeSeriesId, timestamp, value);

        addCallback(publisher.publishDataPoint(metric, timestamp, value, tags, timeSeriesId),
            new PluginError(publisher));

        return result;
      }
    }

    final Timer.Context time = addDataPointTimer.time();

    final ListenableFuture<Void> addPointComplete = Futures.transform(
        labelClient.getTimeSeriesId(metric, tags), new AddPointFunction());

    StopTimerCallback.stopOn(time, addPointComplete);

    return addPointComplete;
  }

  /**
   * Add a double precision floating point data point at the provided timestamp that will belong to
   * the time series behind the given metric and tags. See {@link #checkMetricAndTags(String, Map)}
   * for the contract that the metric and tags must fulfill and see {@link
   * Timestamps#checkTimestamp(long)} for the contract that the timestamp must fulfill.
   *
   * @return A future that indicates the completion of the request or an error.
   */
  public ListenableFuture<Void> addPoint(final String metric,
                                         final long timestamp,
                                         final double value,
                                         final Map<String, String> tags) {
    Timestamps.checkTimestamp(timestamp);
    checkMetricAndTags(metric, tags);

    class AddPointFunction implements AsyncFunction<TimeSeriesId, Void> {
      @Override
      public ListenableFuture<Void> apply(final TimeSeriesId timeSeriesId) {
        ListenableFuture<Void> result = store.addPoint(timeSeriesId, timestamp, value);

        addCallback(publisher.publishDataPoint(metric, timestamp, value, tags, timeSeriesId),
            new PluginError(publisher));

        return result;
      }
    }

    final Timer.Context time = addDataPointTimer.time();

    final ListenableFuture<Void> addPointComplete = Futures.transform(
        labelClient.getTimeSeriesId(metric, tags), new AddPointFunction());

    StopTimerCallback.stopOn(time, addPointComplete);

    return addPointComplete;
  }

  /**
   * Add a long integral data point at the provided timestamp that will belong to the time series
   * behind the given metric and tags. See {@link #checkMetricAndTags(String, Map)} for the contract
   * that the metric and tags must fulfill and see {@link Timestamps#checkTimestamp(long)} for the
   * contract that the timestamp must fulfill.
   *
   * @return A future that indicates the completion of the request or an error.
   */
  public ListenableFuture<Void> addPoint(final String metric,
                                         final long timestamp,
                                         final long value,
                                         final Map<String, String> tags) {
    Timestamps.checkTimestamp(timestamp);
    checkMetricAndTags(metric, tags);

    class AddPointFunction implements AsyncFunction<TimeSeriesId, Void> {
      @Override
      public ListenableFuture<Void> apply(final TimeSeriesId timeSeriesId) {
        ListenableFuture<Void> result = store.addPoint(timeSeriesId, timestamp, value);

        addCallback(publisher.publishDataPoint(metric, timestamp, value, tags, timeSeriesId),
            new PluginError(publisher));

        return result;
      }
    }

    final Timer.Context time = addDataPointTimer.time();

    final ListenableFuture<Void> addPointComplete = Futures.transform(
        labelClient.getTimeSeriesId(metric, tags), new AddPointFunction());

    StopTimerCallback.stopOn(time, addPointComplete);

    return addPointComplete;
  }

  @Nonnull
  public Iterator<TimeSeriesResultSet> query(final String query) {
    return null;
  }
}
