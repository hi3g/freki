package se.tre.freki.storage.cassandra.functions;


import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.List;
import javax.annotation.Nonnull;

public class FirstOrAbsentFunction<V> implements Function<List<V>, Optional<V>> {
  @Nonnull
  @Override
  public Optional<V> apply(final List<V> list) {
    if (list.isEmpty()) {
      return Optional.absent();
    } else {
      return Optional.of(list.get(0));
    }
  }
}
