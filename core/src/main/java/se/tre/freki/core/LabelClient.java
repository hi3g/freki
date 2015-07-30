package se.tre.freki.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.transform;

import se.tre.freki.labels.IdLookupStrategy;
import se.tre.freki.labels.LabelClientTypeContext;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.labels.Labels;
import se.tre.freki.labels.StaticTimeSeriesId;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.search.IdChangeIndexerListener;
import se.tre.freki.search.SearchPlugin;
import se.tre.freki.storage.Store;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.typesafe.config.Config;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LabelClient {
  /** Label type context for the metric names. */
  final LabelClientTypeContext metrics;
  /** Label type context for the tag names. */
  final LabelClientTypeContext tagKeys;
  /** Label type context for the tag values. */
  final LabelClientTypeContext tagValues;

  private final IdLookupStrategy tagKeyLookupStrategy;
  private final IdLookupStrategy tagValueLookupStrategy;
  private final IdLookupStrategy metricLookupStrategy;

  /**
   * Create a new instance using the given non-null arguments to configure itself.
   */
  @Inject
  public LabelClient(final Store store,
                     final Config config,
                     final MetricRegistry metricsRegistry,
                     final EventBus idEventBus,
                     final SearchPlugin searchPlugin) {
    checkNotNull(config);
    checkNotNull(store);

    tagKeyLookupStrategy = lookupStrategy(
        config.getBoolean("freki.core.auto_create_tagks"));
    tagValueLookupStrategy = lookupStrategy(
        config.getBoolean("freki.core.auto_create_tagvs"));
    metricLookupStrategy = lookupStrategy(
        config.getBoolean("freki.core.auto_create_metrics"));

    metrics = new LabelClientTypeContext(store, LabelType.METRIC, metricsRegistry, idEventBus,
        config.getLong("freki.core.metrics.cache.max_size"));
    tagKeys = new LabelClientTypeContext(store, LabelType.TAGK, metricsRegistry, idEventBus,
        config.getLong("freki.core.tag_keys.cache.max_size"));
    tagValues = new LabelClientTypeContext(store, LabelType.TAGV, metricsRegistry, idEventBus,
        config.getLong("freki.core.tag_values.cache.max_size"));

    // Notify the search plugin about new and deleted labels
    idEventBus.register(new IdChangeIndexerListener(store, searchPlugin));
  }

  /**
   * Get a fitting {@link IdLookupStrategy} based on whether IDs should be created if they exist or
   * not.
   *
   * @param shouldCreate Whether the returned lookup strategy should create missing IDs or not
   * @return A fitting instantiated {@link IdLookupStrategy}
   */
  private IdLookupStrategy lookupStrategy(final boolean shouldCreate) {
    if (shouldCreate) {
      return IdLookupStrategy.CreatingIdLookupStrategy.instance;
    }

    return IdLookupStrategy.SimpleIdLookupStrategy.instance;
  }

  /**
   * Takes a {@link String} name and a {@link LabelType} and executes a lookup
   * for the corresponding {@link LabelId} and returns it as {@link ListenableFuture}.
   *
   * @param name The name of the label.
   * @param type The type of the label.
   *
   * @return A {@link ListenableFuture} with a {@link LabelId}.
   */
  public ListenableFuture<LabelId> lookupId(final String name, final LabelType type) {
    switch (type) {
      case METRIC:
        return metricLookupStrategy.getId(metrics, name);
      case TAGK:
        return tagKeyLookupStrategy.getId(metrics, name);
      case TAGV:
        return tagValueLookupStrategy.getId(metrics, name);
      default:
        throw new IllegalArgumentException(type + " is unknown");
    }
  }

  /**
   * Get the IDs for all tag keys and tag values in the provided {@link java.util.Map} using the
   * provided tag key and tag value {@link IdLookupStrategy}. The returned value is a future that on
   * completion contains a list of striped IDs with the tag key ID on odd indexes and tag value IDs
   * on even indexes.
   *
   * @param tags The names for which to lookup the IDs for
   * @param tagKeyStrategy The strategy to use for looking up tag keys
   * @param tagValueStrategy The strategy to use for looking up tag values
   * @return A future that on completion contains a striped list of all IDs
   */
  @Nonnull
  private ListenableFuture<List<LabelId>> getTagIds(final Map<String, String> tags,
                                                    final IdLookupStrategy tagKeyStrategy,
                                                    final IdLookupStrategy tagValueStrategy) {
    final ImmutableList.Builder<ListenableFuture<LabelId>> tagIds = ImmutableList.builder();

    // For each tag, start resolving the tag name and the tag value.
    for (final Map.Entry<String, String> entry : tags.entrySet()) {
      tagIds.add(tagKeyStrategy.getId(tagKeys, entry.getKey()));
      tagIds.add(tagValueStrategy.getId(tagValues, entry.getValue()));
    }

    return allAsList(tagIds.build());
  }

  /**
   * Lookup the label context for the provided type.
   *
   * @param type The label type to lookup the context for
   * @return The label context for the provided type
   */
  public LabelClientTypeContext contextForType(LabelType type) {
    switch (type) {
      case METRIC:
        return metrics;
      case TAGK:
        return tagKeys;
      case TAGV:
        return tagValues;
      default:
        throw new IllegalArgumentException(type + " is unknown");
    }
  }

  /**
   * Get the label context for metrics.
   */
  public LabelClientTypeContext metricContext() {
    return metrics;
  }

  /**
   * Get the label context for tag keys.
   */
  public LabelClientTypeContext tagKeyContext() {
    return tagKeys;
  }

  /**
   * Get the label context for tag values.
   */
  public LabelClientTypeContext tagValueContext() {
    return tagValues;
  }

  /**
   * Create a new ID for the given name and type. The name must pass the checks performed by {@link
   * Labels#checkLabelName(String, String)}.
   *
   * @param type The type of label to create
   * @param name The name of the label to create
   * @return A future that on completion will either contain the newly created ID or an error
   */
  @Nonnull
  public ListenableFuture<LabelId> createId(final LabelType type,
                                            final String name) {
    Labels.checkLabelName(type.toString(), name);

    final LabelClientTypeContext instance = contextForType(type);

    return transform(instance.checkUidExists(name),
        new AsyncFunction<Boolean, LabelId>() {
          @Override
          public ListenableFuture<LabelId> apply(final Boolean exists) throws Exception {
            if (exists) {
              return Futures.immediateFailedFuture(
                  new IllegalArgumentException("Name already exists"));
            }

            return instance.createId(name);
          }
        });
  }

  /**
   * Lookup the name behind the provided label ID and label type.
   *
   * @param type The type of label
   * @param uid The ID to search for
   * @return A future that on completion will contain the name behind the ID
   */
  @Nonnull
  public ListenableFuture<Optional<String>> getLabelName(final LabelType type,
                                                         final LabelId uid) {
    checkNotNull(uid, "Missing UID");
    LabelClientTypeContext labelClientTypeContext = contextForType(type);
    return labelClientTypeContext.getName(uid);
  }

  /**
   * Lookup the label ID behind the provided name and label type.
   *
   * @param type The type of label to lookup
   * @param name The name to search for
   * @return A future that on completion will contain the ID behind the name
   */
  @Nonnull
  public ListenableFuture<Optional<LabelId>> getLabelId(final LabelType type,
                                                        final String name) {
    checkArgument(!Strings.isNullOrEmpty(name), "Missing label name");
    LabelClientTypeContext labelClientTypeContext = contextForType(type);
    return labelClientTypeContext.getId(name);
  }

  /**
   * Returns an initialized {@link TimeSeriesId} for this metric and these tags.
   *
   * @param metric The metric to use in the TSUID
   * @param tags The string tags to use in the TSUID
   */
  ListenableFuture<TimeSeriesId> getTimeSeriesId(final String metric,
                                                 final Map<String, String> tags) {
    // Use the configured metric lookup strategy to get the id behind the name
    final ListenableFuture<LabelId> metric_id = metricLookupStrategy.getId(metrics, metric);

    class BuildTimeSeriesIdFunction implements Function<LabelId, TimeSeriesId> {
      private final List<LabelId> tagIds;

      public BuildTimeSeriesIdFunction(final List<LabelId> tagIds) {
        this.tagIds = tagIds;
      }

      @Override
      public TimeSeriesId apply(final LabelId metricid) {
        return new StaticTimeSeriesId(metricid, tagIds);
      }
    }

    class TransformToTimeSeriesId implements AsyncFunction<List<LabelId>, TimeSeriesId> {
      @Override
      public ListenableFuture<TimeSeriesId> apply(final List<LabelId> tags) {
        return transform(metric_id, new BuildTimeSeriesIdFunction(tags));
      }
    }

    // Start resolving all tags
    return transform(getTagIds(tags, tagKeyLookupStrategy, tagValueLookupStrategy),
        new TransformToTimeSeriesId());
  }
}
