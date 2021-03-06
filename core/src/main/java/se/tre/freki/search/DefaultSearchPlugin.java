package se.tre.freki.search;

import se.tre.freki.BuildData;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.meta.Annotation;
import se.tre.freki.meta.LabelMeta;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nonnull;

/**
 * A default search plugin to use when no other search plugin has been configured. This search
 * plugin will just discard all data given to it.
 */
public class DefaultSearchPlugin extends SearchPlugin {
  @Override
  public void close() {
  }

  @Override
  public String version() {
    return BuildData.version();
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> indexLabelMeta(final LabelMeta meta) {
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteLabelMeta(final LabelId id,
                                                final LabelType type) {
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> indexAnnotation(final Annotation note) {
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Void> deleteAnnotation(final Annotation note) {
    return Futures.immediateFuture(null);
  }

  @Nonnull
  @Override
  public ListenableFuture<Iterable<LabelMeta>> findLabels(final String query) {
    return Futures.<Iterable<LabelMeta>>immediateFuture(ImmutableSet.<LabelMeta>of());
  }
}
