package se.tre.freki.plugins;

import se.tre.freki.BuildData;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.meta.Annotation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;

/**
 * A default realtime publisher to use when no other real-time publisher has been configured. This
 * real-time publisher will just discard all data given to it.
 */
public class DefaultRealtimePublisher extends RealTimePublisher {
  @Override
  public void close() {
  }

  @Override
  public String version() {
    return BuildData.version();
  }

  @Override
  public ListenableFuture<Void> publishDataPoint(final String metric,
                                                 final long timestamp,
                                                 final long value,
                                                 final Map<String, String> tags,
                                                 final TimeSeriesId timeSeriesId) {
    return Futures.immediateFuture(null);
  }

  @Override
  public ListenableFuture<Void> publishDataPoint(final String metric,
                                                 final long timestamp,
                                                 final double value,
                                                 final Map<String, String> tags,
                                                 final TimeSeriesId timeSeriesId) {
    return Futures.immediateFuture(null);
  }

  @Override
  public ListenableFuture<Void> publishAnnotation(final Annotation annotation) {
    return Futures.immediateFuture(null);
  }
}
