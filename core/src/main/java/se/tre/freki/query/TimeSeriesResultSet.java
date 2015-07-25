package se.tre.freki.query;

import com.google.common.collect.ImmutableSet;

import java.util.Iterator;
import javax.annotation.Nonnull;

public interface TimeSeriesResultSet {
  @Nonnull
  ImmutableSet<DecoratedTimeSeriesId> timeSeriesIds();

  @Nonnull
  Iterator<Number> values();

  @Nonnull
  Iterator<Long> timestamps();
}
