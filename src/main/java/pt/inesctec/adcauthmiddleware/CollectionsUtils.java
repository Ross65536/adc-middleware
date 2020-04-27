package pt.inesctec.adcauthmiddleware;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CollectionsUtils {
  public static <E, K, V> Map<K, V> toMapKeyByLatest(
      Collection<E> stream, Function<E, K> keyBuilder, Function<E, V> valueBuilder) {
    var map = new HashMap<K, V>();

    stream.forEach(e -> map.put(keyBuilder.apply(e), valueBuilder.apply(e)));

    return map;
  }

  public static <T> String toString(List<T> list) {
    return "["
        + list.stream().map(e -> String.format("'%s'", e)).collect(Collectors.joining(", "))
        + "]";
  }

  public static <T> T[] toArray(List<T> list) {
    return (T[]) list.toArray();
  }
}
