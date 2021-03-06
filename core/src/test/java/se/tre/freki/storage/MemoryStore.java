package se.tre.freki.storage;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.meta.Annotation;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.query.DataPoint;
import se.tre.freki.query.TimeSeriesQuery;
import se.tre.freki.utils.AsyncIterator;

import com.codahale.metrics.MetricRegistry;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import javax.annotation.Nonnull;

public class MemoryStore extends Store {
  private final Table<LabelId, String, LabelMeta> labelMetas;
  private final Table<TimeSeriesKey, Long, Annotation> annotations;

  private final Map<TimeSeriesId, NavigableMap<Long, Number>> datapoints;

  private final Table<String, LabelType, LabelId> identifierForward;
  private final Table<LabelId, LabelType, String> identifierReverse;

  public MemoryStore() {
    labelMetas = HashBasedTable.create();
    annotations = HashBasedTable.create();
    identifierForward = HashBasedTable.create();
    identifierReverse = HashBasedTable.create();
    datapoints = Maps.newHashMap();
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                         final long timestamp,
                                         final float value) {
    return addPoint(tsuid, value, timestamp);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                         final long timestamp,
                                         final double value) {
    return addPoint(tsuid, value, timestamp);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                         final long timestamp,
                                         final long value) {
    return addPoint(tsuid, (Number) value, timestamp);
  }

  private ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                          final Number value,
                                          final long timestamp) {
    /*
     * TODO(luuse): tsuid neither implements #equals, #hashCode or Comparable.
     * Should implement a custom TimeSeriesId for MemoryStore that implements all
     * of these.
     */
    NavigableMap<Long, Number> tsuidDps = datapoints.get(tsuid);

    if (tsuidDps == null) {
      tsuidDps = Maps.newTreeMap();
      datapoints.put(tsuid, tsuidDps);
    }

    tsuidDps.put(timestamp, value);

    return Futures.immediateFuture(null);
  }

  @Override
  public void close() {
  }

  @Nonnull
  @Override
  public ListenableFuture<Optional<LabelId>> getId(String name,
                                                   LabelType type) {
    LabelId id = identifierForward.get(name, type);
    return Futures.immediateFuture(Optional.fromNullable(id));
  }

  @Nonnull
  @Override
  public ListenableFuture<Optional<String>> getName(final LabelId id,
                                                    final LabelType type) {
    final String name = identifierReverse.get(id, type);
    return Futures.immediateFuture(Optional.fromNullable(name));
  }

  @Nonnull
  @Override
  public ListenableFuture<Optional<LabelMeta>> getMeta(final LabelId uid,
                                                       final LabelType type) {
    final String qualifier = type.toString().toLowerCase() + "_meta";
    final LabelMeta meta = labelMetas.get(uid, qualifier);
    return Futures.immediateFuture(Optional.fromNullable(meta));
  }

  @Override
  public void registerMetricsWith(final MetricRegistry registry) {
    // These are not the metrics you are looking for.
  }

  @Nonnull
  @Override
  public ListenableFuture<Boolean> updateMeta(final LabelMeta meta) {
    labelMetas.put(
        meta.identifier(),
        meta.type().toString().toLowerCase() + "_meta",
        meta);

    return Futures.immediateFuture(Boolean.TRUE);
  }

  @Nonnull
  @Override
  public ListenableFuture<Map<TimeSeriesId, AsyncIterator<? extends DataPoint>>> query(
      final TimeSeriesQuery query) {
    throw new UnsupportedOperationException("Not implemented yet!");
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteLabel(final String name, LabelType type) {
    identifierForward.remove(name, type);
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<LabelId> createLabel(final String name,
                                               final LabelType type) {
    LabelId id;

    do {
      id = new MemoryLabelId();
      // Make sure the new id is unique
    } while (identifierReverse.containsRow(id));

    identifierReverse.put(id, type, name);
    identifierForward.put(name, type, id);

    return Futures.immediateFuture(id);
  }

  @Nonnull
  @Override
  public ListenableFuture<Boolean> renameLabel(final String name,
                                               final LabelId id,
                                               final LabelType type) {

    identifierReverse.put(id, type, name);

    if (identifierForward.contains(name, type)) {
      return Futures.immediateFuture(identifierForward.contains(name, type));
    }

    identifierForward.put(name, type, id);

    return Futures.immediateFuture(true);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteAnnotation(final LabelId metric,
                                                 final ImmutableMap<LabelId, LabelId> tags,
                                                 final long startTime) {
    annotations.remove(TimeSeriesKey.create(metric, tags), startTime);
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Boolean> updateAnnotation(Annotation annotation) {
    final TimeSeriesKey row = TimeSeriesKey.create(annotation.metric(), annotation.tags());
    final Annotation changedAnnotation = annotations.put(row, annotation.startTime(), annotation);
    return Futures.immediateFuture(!annotation.equals(changedAnnotation));
  }

  @Nonnull
  @Override
  public ListenableFuture<Integer> deleteAnnotations(final LabelId metric,
                                                     final ImmutableMap<LabelId, LabelId> tags,
                                                     final long startTime,
                                                     final long endTime) {
    final TimeSeriesKey row = TimeSeriesKey.create(metric, tags);
    final ArrayList<Annotation> removedAnnotations = new ArrayList<>();

    final Collection<Annotation> matchedAnnotations = annotations.row(row).values();
    for (final Annotation matchedAnnotation : matchedAnnotations) {
      if (startTime <= matchedAnnotation.startTime() && matchedAnnotation.startTime() <= endTime) {
        removedAnnotations.add(matchedAnnotation);
      }
    }

    for (final Annotation annotation : removedAnnotations) {
      deleteAnnotation(annotation.metric(), annotation.tags(), annotation.startTime());
    }

    return Futures.immediateFuture(removedAnnotations.size());
  }

  @Nonnull
  @Override
  public ListenableFuture<Annotation> getAnnotation(final LabelId metric,
                                                    final ImmutableMap<LabelId, LabelId> tags,
                                                    final long startTime) {
    final TimeSeriesKey row = TimeSeriesKey.create(metric, tags);
    return Futures.immediateFuture(annotations.get(row, startTime));
  }

  /**
   * Class used as the key in tables to represent a combined time series ID.
   */
  @AutoValue
  abstract static class TimeSeriesKey {
    static TimeSeriesKey create(final LabelId metric,
                                final Map<LabelId, LabelId> tags) {
      return new AutoValue_MemoryStore_TimeSeriesKey(metric, ImmutableMap.copyOf(tags));
    }

    @Nonnull
    abstract LabelId metric();

    @Nonnull
    abstract ImmutableMap<LabelId, LabelId> tags();
  }

}
