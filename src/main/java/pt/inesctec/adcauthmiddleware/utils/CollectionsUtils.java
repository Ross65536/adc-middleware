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
  /**
   * Turns a stream of objects to a map.
   *
   * @param stream the source elements
   * @param keyBuilder function to obtain key from element
   * @param valueBuilder function to obtain value from element
   * @param <E> element
   * @param <K> key
   * @param <V> value
   * @return the built modifiable map
   */
  public static <E, K, V> Map<K, V> toMapKeyByLatest(
      Collection<E> stream, Function<E, K> keyBuilder, Function<E, V> valueBuilder) {
    var map = new HashMap<K, V>();

    stream.forEach(e -> map.put(keyBuilder.apply(e), valueBuilder.apply(e)));

    return map;
  }

  /**
   * Returns string representation of a collection.
   *
   * @param list the source collection
   * @param <T> the element type
   * @return the string representation
   */
  public static <T> String toString(Collection<T> list) {
    if (list == null) {
      return "[]";
    }

    return "["
        + list.stream().map(e -> String.format("'%s'", e)).collect(Collectors.joining(", "))
        + "]";
  }

  /**
   * Allows asserting for each element in collection.
   *
   * @param list the source collection
   * @param checker the assertion function
   * @param errorMsg error message for exception
   * @param <T> collection type
   * @throws Exception the exception on failed assertion
   */
  public static <T> void assertList(Iterable<T> list, Function<T, Boolean> checker, String errorMsg)
      throws Exception {
    for (T e : list) {
      if (!checker.apply(e)) {
        throw new Exception(errorMsg);
      }
    }
  }

  /**
   * Checks that the collection contains the specified list of elements.
   *
   * @param list the source collection
   * @param elements list to check against
   * @param <T> element type
   * @throws Exception when an element is not present
   */
  public static <T> void assertListContains(Collection<T> list, T... elements) throws Exception {
    for (T e : elements) {
      if (!list.contains(e)) {
        throw new Exception("list missing " + e);
      }
    }
  }

  /**
   * Checks that map contains the specified list of keys.
   *
   * @param list source map
   * @param fields the keys to check against
   * @param <T> key type
   * @param <V> value type, unused
   * @throws Exception when a key is missing
   */
  public static <T, V> void assertMapListContainsKeys(Collection<Map<T, V>> list, T... fields) throws Exception {
    for (var map : list) {
      for (T field : fields) {
        if (!map.containsKey(field)) {
          throw new Exception("list object missing field " + field);
        }
      }
    }
  }

  /**
   * Build mutable triple nested map from collection.
   *
   * @param list source collection
   * @param keyOuterFunc outer ket getter from collection element
   * @param keyMidFunc middle key getter from collection element
   * @param keyInnerFunc inner key getter from collection element
   * @param <E> outermost value type
   * @param <K1> outer key type
   * @param <K2> middle key type
   * @param <K3> innermost key type
   * @return the nested map
   */
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

  /**
   * Turns list to set. Useful for adding nulls values.
   *
   * @param values source value list.
   * @param <K> type
   * @return built set
   */
  public static <K> Set<K> toSet(K... values) {
    return Arrays.stream(values).collect(Collectors.toSet());
  }

  /**
   * Returns copy of of array without front element.
   *
   * @param elems source array.
   * @param <E> type
   * @return array copy
   */
  public static <E> E[] popFront(E[] elems) {
    return Arrays.copyOfRange(elems, 1, elems.length);
  }

  /**
   * Returns set with only one element: null value.
   *
   * @param <T> set type
   * @return set
   */
  public static <T> Set<T> immutableSetWithNull() {
    var set = new HashSet<T>();
    set.add(null);

    return Collections.unmodifiableSet(set);
  }

  /**
   * Remove values that are maps from the source map.
   *
   * @param map source map
   * @param <K> key type
   * @param <V> value type
   */
  public static <K, V> void stripNestedMaps(Map<K, V> map) {
    map.entrySet().removeIf(e -> e.getValue() instanceof Map);
  }

  /**
   * Returns copy of map whose keys match the elements in the set.
   *
   * @param allFieldTypes source map
   * @param requestedFields keys to match against
   * @param <K> key type
   * @param <V> value type
   * @return map subset copy
   */
  public static <K,V> Map<K, V> intersectMapWithSet(
      Map<K, V> allFieldTypes, Set<K> requestedFields) {
    return allFieldTypes.entrySet().stream()
        .filter(e -> requestedFields.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
