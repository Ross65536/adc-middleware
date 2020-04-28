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

  public static <T> void assertList(Iterable<T> list, Function<T, Boolean> checker, String errorMsg)
      throws Exception {
    for (T e : list) {
      if (!checker.apply(e)) {
        throw new Exception(errorMsg);
      }
    }
  }

  public static <E, K1, K2, K3> Map<K1, Map<K2, Map<K3, E>>> buildMap(
      Collection<E> list, Function<E, K1> keyOuterFunc, Function<E, K2> keyMidFunc, Function<E, K3> keyInnerFunc) {
    var topMap = new HashMap<K1, Map<K2, Map<K3, E>>>();

    list.forEach(e -> {
      var key1 = keyOuterFunc.apply(e);
      var key2 = keyMidFunc.apply(e);
      var key3 = keyInnerFunc.apply(e);


      var innerMap = topMap.computeIfAbsent(key1, k -> new HashMap<>());
      var innermostMap = innerMap.computeIfAbsent(key2, k -> new HashMap<>());

      innermostMap.put(key3, e);
    });

    return topMap;
  }
}
