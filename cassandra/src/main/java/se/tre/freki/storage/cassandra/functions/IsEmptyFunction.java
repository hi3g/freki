package se.tre.freki.storage.cassandra.functions;

import com.google.common.base.Function;

import java.util.Collection;
import javax.annotation.Nonnull;

public class IsEmptyFunction implements Function<Collection, Boolean> {
  @Nonnull
  @Override
  public Boolean apply(final Collection input) {
    return input.isEmpty();
  }
}
