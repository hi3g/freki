package se.tre.freki.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Futures.transform;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.Annotation;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.plugins.PluginError;
import se.tre.freki.plugins.RealTimePublisher;
import se.tre.freki.search.SearchPlugin;
import se.tre.freki.storage.Store;
import se.tre.freki.time.TimeRanges;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The exposed interface for managing meta objects.
 */
@Singleton
public class MetaClient {
  private static final Logger LOG = LoggerFactory.getLogger(MetaClient.class);

  private final Store store;
  private final LabelClient labelClient;
  private final SearchPlugin searchPlugin;
  private final RealTimePublisher realTimePublisher;

  /**
   * Create a new instance using the given arguments to configure itself.
   */
  @Inject
  public MetaClient(final Store store,
                    final SearchPlugin searchPlugin,
                    final LabelClient labelClient,
                    final RealTimePublisher realTimePublisher) {
    this.store = checkNotNull(store);
    this.labelClient = checkNotNull(labelClient);
    this.searchPlugin = checkNotNull(searchPlugin);
    this.realTimePublisher = checkNotNull(realTimePublisher);
  }

  public void deleteAnnotation(final Annotation note) {
    addCallback(searchPlugin.deleteAnnotation(note), new PluginError(searchPlugin));
  }

  /**
   * Perform a query using the configured search plugin and get the matching label meta objects.
   *
   * @param query A plugin specific query as received without modification
   * @return A future that on completion contains an {@link Iterable} of label meta objects
   * @see SearchPlugin#findLabels(String) for information about the format of the query
   */
  @Nonnull
  public ListenableFuture<Iterable<LabelMeta>> findLabels(final String query) {
    return searchPlugin.findLabels(query);
  }

  /**
   * Get the annotation behind the timeseries indicated by the provided metric and tags that starts
   * at the exact timestamp indicated by {@code timestamp}. Annotations that matches the metric and
   * tags and whose {@code startTime < timestamp AND endTime > timestamp} but whose {@code
   * startTime} does not exactly equal {@code timestamp} will not be returned.
   *
   * @param metric The metric the annotation belongs to
   * @param tags The tags the annotation belongs to
   * @param timestamp The exact {@link Annotation#startTime()} of the desired annotation
   * @return A future that on completion contains the annotation
   */
  @Nonnull
  public ListenableFuture<Annotation> getAnnotation(final LabelId metric,
                                                    final ImmutableMap<LabelId, LabelId> tags,
                                                    final long timestamp) {
    checkNotNull(metric);
    checkArgument(!tags.isEmpty());
    TimeRanges.checkStartTime(timestamp);

    return store.getAnnotation(metric, tags, timestamp);
  }

  /**
   * Delete the annotation behind the timeseries indicated by the provided metric and tags that
   * starts at the exact timestamp indicated by {@code timestamp}. Annotations that matches the
   * metric and tags and whose {@code startTime < timestamp AND endTime > timestamp} but whose
   * {@code startTime} does not exactly equal {@code timestamp} will not be deleted.
   *
   * @param metric The metric the annotation belongs to
   * @param tags The tags the annotation belongs to
   * @param timestamp The exact {@link Annotation#startTime()} of the annotation to be deleted
   * @return A future that indicated the completion of the request
   */
  @Nonnull
  public ListenableFuture<Void> delete(final LabelId metric,
                                       final ImmutableMap<LabelId, LabelId> tags,
                                       final long timestamp) {
    checkNotNull(metric);
    checkArgument(!tags.isEmpty(), "At least one tag is required");
    TimeRanges.checkStartTime(timestamp);

    return store.deleteAnnotation(metric, tags, timestamp);
  }

  /**
   * Get the label meta information stored about the label with the provided information.
   *
   * @param type The type of the label to lookup the meta information about
   * @param id The id of the label to lookup the meta information about
   * @return A future that on completion contains the meta object
   */
  public ListenableFuture<Optional<LabelMeta>> getLabelMeta(final LabelType type,
                                                            final LabelId id) {
    return store.getMeta(id, type);
  }

  /**
   * Update the information stored about the annotation with the identifying details of the provided
   * annotation with updated fields.
   *
   * @param annotation The annotation whose information to update
   * @return A future that indicates if any information was changed
   */
  public ListenableFuture<Boolean> updateAnnotation(final Annotation annotation) {
    return Futures.transform(
        store.getAnnotation(annotation.metric(), annotation.tags(), annotation.startTime()),
        new AsyncFunction<Annotation, Boolean>() {
          @Override
          public ListenableFuture<Boolean> apply(final Annotation storedAnnotation)
              throws Exception {
            if (!storedAnnotation.equals(annotation)) {
              return store.updateAnnotation(annotation);
            }

            LOG.debug("{} does not have any changes, skipping update", annotation);
            return Futures.immediateFuture(Boolean.FALSE);
          }
        });
  }

  /**
   * Delete all annotations behind the timeseries indicated by the provided metric and tags whose
   * start time or end time fall within the provided {@code startTime} and {@code endTime}.
   *
   * @param metric The metric the annotation belongs to
   * @param tags The tags the annotation belongs to
   * @param startTime The lower bound where to start deleting annotations
   * @param endTime The upper bound where to end deleting annotations
   * @return A future that indicates the number of annotations that were deleted
   */
  @Nonnull
  public ListenableFuture<Integer> deleteRange(final LabelId metric,
                                               final ImmutableMap<LabelId, LabelId> tags,
                                               final long startTime,
                                               final long endTime) {
    checkNotNull(metric, "Missing a metric", metric, tags);
    checkArgument(!tags.isEmpty(), "At least one tag is required", metric, tags);
    TimeRanges.checkFiniteTimeRange(startTime, endTime);

    return store.deleteAnnotations(metric, tags, startTime, endTime);
  }

  /**
   * Attempts to update the information of the stored LabelMeta object with the same {@code
   * identifier} and {@code type} as the provided meta object. The provided meta object will be
   * checked for changes against the stored object before saving anything.
   *
   * @param meta The LabelMeta with the updated information.
   * @return A future that on completion contains a {@code true} if the update was successful or
   * {@code false} if the meta object had no changes to save.
   * @throws se.tre.freki.labels.LabelException If the UID does not exist
   */
  public ListenableFuture<Boolean> update(final LabelMeta meta) {
    return transform(getLabelMeta(meta.type(), meta.identifier()),
        new AsyncFunction<Optional<LabelMeta>, Boolean>() {
          @Override
          public ListenableFuture<Boolean> apply(final Optional<LabelMeta> storedMeta)
              throws Exception {
            if (!storedMeta.isPresent()) {
              return store.updateMeta(meta);
            }

            if (!storedMeta.get().equals(meta)) {
              return store.updateMeta(meta);
            }
            LOG.debug("{} does not have any changes, skipping update", meta);
            return Futures.immediateFuture(Boolean.FALSE);
          }
        });
  }
}
