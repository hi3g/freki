package se.tre.freki.storage;

import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.meta.Annotation;
import se.tre.freki.meta.LabelMeta;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * An abstract class defining the functions any database used with Freki must implement. Another
 * requirement is that the database connection has to be asynchronous.
 */
public abstract class Store implements Closeable {
  //
  // Identifier management
  //
  @Nonnull
  public abstract ListenableFuture<LabelId> createLabel(final String name,
                                                        final LabelType type);

  /**
   * Renames a old LabelId to the new {@code newName}. This method makes no checks if this is a
   * valid rename. The method calling this function needs to make sure this is done before. There
   * are 2 checks that needs to be done. Firstly we can not rename to a name that already exists.
   * Secondly old {@code LabelId} needs to exist.
   *
   * @param newName The new name that we are renaming to.
   * @param id The old label ID.
   * @param type The label type.
   * @return The LabelId that was used.
   */
  @Nonnull
  public abstract ListenableFuture<Boolean> renameLabel(final String newName,
                                                        final LabelId id,
                                                        final LabelType type);

  @Nonnull
  public abstract ListenableFuture<Void> deleteLabel(final String name, final LabelType type);

  public abstract ListenableFuture<List<byte[]>> executeTimeSeriesQuery(
      final Object query);

  @Nonnull
  public abstract ListenableFuture<Optional<LabelId>> getId(final String name,
                                                            final LabelType type);

  @Nonnull
  public abstract ListenableFuture<Optional<String>> getName(final LabelId id,
                                                             final LabelType type);

  //
  // Datapoints
  //
  @Nonnull
  public abstract ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                                  final long timestamp,
                                                  final float value);

  @Nonnull
  public abstract ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                                  final long timestamp,
                                                  final double value);

  @Nonnull
  public abstract ListenableFuture<Void> addPoint(final TimeSeriesId tsuid,
                                                  final long timestamp,
                                                  final long value);

  // TODO
  public abstract ListenableFuture<ImmutableList<Object>> executeQuery(final Object query);

  //
  // Annotations
  //

  @Nonnull
  public abstract ListenableFuture<Void> deleteAnnotation(final LabelId metric,
                                                          final ImmutableMap<LabelId, LabelId> tags,
                                                          final long startTime);

  @Nonnull
  public abstract ListenableFuture<Integer> deleteAnnotations(
      final LabelId metric,
      final ImmutableMap<LabelId, LabelId> tags,
      final long startTime,
      final long endTime);

  @Nonnull
  public abstract ListenableFuture<Annotation> getAnnotation(
      final LabelId metric,
      final ImmutableMap<LabelId, LabelId> tags,
      final long startTime);

  @Nonnull
  public abstract ListenableFuture<Boolean> updateAnnotation(final Annotation annotation);

  //
  // LabelMeta
  //
  @Nonnull
  public abstract ListenableFuture<LabelMeta> getMeta(final LabelId uid,
                                                      final LabelType type);

  @Nonnull
  public abstract ListenableFuture<Boolean> updateMeta(final LabelMeta meta);
}
