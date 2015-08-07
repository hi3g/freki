package se.tre.freki.utils;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.transform;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MoreMaps {
  public static <K1, K2, V> ListenableFuture<Map<K2, V>> transformKeys(
      final Map<K1, V> inputMap,
      final AsyncFunction<Map.Entry<K1, V>, ? extends Map.Entry<K2, V>> transformation) {

    final Set<Map.Entry<K1, V>> inputEntries = inputMap.entrySet();
    final List<ListenableFuture<? extends Map.Entry<K2, V>>> transformedEntryFutures =
        new ArrayList<>(inputEntries.size());

    try {
      for (final Map.Entry<K1, V> inputEntry : inputEntries) {
        transformedEntryFutures.add(transformation.apply(inputEntry));
      }

      return transform(allAsList(transformedEntryFutures),
          new Function<List<Map.Entry<K2, V>>, Map<K2, V>>() {
            @Override
            public Map<K2, V> apply(final List<Map.Entry<K2, V>> transformedEntries) {
              final ImmutableMap.Builder<K2, V> transformedMap = ImmutableMap.builder();

              for (final Map.Entry<K2, V> transformedEntry : transformedEntries) {
                transformedMap.put(transformedEntry);
              }

              return transformedMap.build();
            }
          });
    } catch (Exception e) {
      return Futures.immediateFailedFuture(e);
    }
  }
}
