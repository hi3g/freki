package se.tre.freki.storage.cassandra.functions;

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TrueFunction implements Function<Object, Boolean> {
  @Nonnull
  @Override
  public Boolean apply(@Nullable final Object input) {
    return Boolean.TRUE;
  }
}
