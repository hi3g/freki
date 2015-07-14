package se.tre.freki.storage.cassandra.functions;

import com.google.common.base.Function;

import javax.annotation.Nullable;

public class ToVoidFunction implements Function<Object, Void> {
  @Nullable
  @Override
  public Void apply(@Nullable final Object object) {
    return null;
  }
}
