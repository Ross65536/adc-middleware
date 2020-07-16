package pt.inesctec.adcauthmiddleware.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CollectionsUtils {
  public static <E, K, V> Map<K, V> toMapKeyByLatest(
      Collection<E> stream, Function<E, K> keyBuilder, Function<E, V> valueBuilder) {
    var map = new HashMap<K, V>();

    stream.forEach(e -> map.put(keyBuilder.apply(e), valueBuilder.apply(e)));

    return map;
  }

  public static <T> String toString(Collection<T> list) {
    if (list == null) {
      return "[]";
    }

    return "["
        + list.stream().map(e -> String.format("'%s'", e)).collect(Collectors.joining(", "))
        + "]";
  }

  public static <T> void assertList(Iterable<T> list, Function<T, Boolean> checker, String errorMsg)
      throws Exception {
    for (T e : list) {
      if (!checker.apply(e)) {
        throw new Exception(errorMsg);
      }
    }
  }

  public static <T> void assertListContains(Collection<T> list, T... elements) throws Exception {
    for (T e : elements) {
      if (!list.contains(e)) {
        throw new Exception("list missing " + e);
      }
    }
  }

  public static <T, V> void assertMapListContainsKeys(Collection<Map<T, V>> list, T... fields) throws Exception {
    for (var map : list) {
      for (T field : fields) {
        if (!map.containsKey(field)) {
          throw new Exception("list object missing field " + field);
        }
      }
    }
  }

  public static <E, K1, K2, K3> Map<K1, Map<K2, Map<K3, E>>> buildMap(
      Collection<E> list,
      Function<E, K1> keyOuterFunc,
      Function<E, K2> keyMidFunc,
      Function<E, K3> keyInnerFunc) {
    var topMap = new HashMap<K1, Map<K2, Map<K3, E>>>();

    list.forEach(
        e -> {
          var key1 = keyOuterFunc.apply(e);
          var key2 = keyMidFunc.apply(e);
          var key3 = keyInnerFunc.apply(e);

          var innerMap = topMap.computeIfAbsent(key1, k -> new HashMap<>());
          var innermostMap = innerMap.computeIfAbsent(key2, k -> new HashMap<>());

          innermostMap.put(key3, e);
        });

    return Collections.unmodifiableMap(topMap);
  }

  public static <K> Set<K> toSet(K... values) {
    return Arrays.stream(values).collect(Collectors.toSet());
  }

  public static <E> E[] popFront(E[] elems) {
    return Arrays.copyOfRange(elems, 1, elems.length);
  }

  public static <T> Set<T> immutableSetWithNull() {
    var set = new HashSet<T>();
    set.add(null);

    return Collections.unmodifiableSet(set);
  }

  public static <K, V> void stripNestedMaps(Map<K, V> map) {
    map.entrySet().removeIf(e -> e.getValue() instanceof Map);
  }

  public static <K,V> Map<K, V> intersectMapWithSet(
      Map<K, V> allFieldTypes, Set<K> requestedFields) {
    return allFieldTypes.entrySet().stream()
        .filter(e -> requestedFields.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
