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
import javax.annotation.Nonnull;

/**
 * Utility methods for {@link Map}s.
 */
public class MoreMaps {
  /**
   * Transform the keys in the input map into either a new value or a completely different type
   * using a potentially asynchronous transformation method.
   *
   * <p>The returned map is a completely new map and does not share any properties with the input
   * map besides having keys and values that are based on the original map. This means that changes
   * in the input map will not be reflected in the output map.
   *
   * @param inputMap The map to transform
   * @param transformation The transformation function that will be applied to the map
   * @param <K1> The type of the key in the input map
   * @param <K2> The type of the key in the output map
   * @param <V> The type of values contained in the map
   * @return A future that on completion will contain a transformed map from the original
   */
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
            public Map<K2, V> apply(@Nonnull final List<Map.Entry<K2, V>> transformedEntries) {
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
