package pt.inesctec.adcauthmiddleware.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class TestCollections {
  public static <K, V> Map<K, V> mapOf(Pair<K, V>... pairs) {
    var map = new HashMap();
    for (var pair : pairs) {
      map.put(pair.first, pair.second);
    }

    return map;
  }

  public static Map<String, Object> mapMerge(Map<String, Object> ... maps) {
    Map<String, Object> ret = new HashMap<>();

    for (var map : maps) {
      ret.putAll(map);
    }

    return ret;
  }

  public static String getString(Map<String, Object> model, String getField) {
    Object field = getField(model, getField);
    return (String) field;
  }

  public static Map mapSubset(Map map, Set<String> keys) {
    Map retMap = new HashMap();

    for (var key : keys) {
      var obj = getField(map, key);
      putMap(retMap, key, obj);
    }

    return retMap;
  }

  private static void putMap(Map map, String key, Object val) {
    var fields = key.split("\\.");
    if (fields.length == 0) {
      throw new IllegalArgumentException("Invalid size");
    }


    for (int i = 0; i < fields.length; i++) {
      boolean isLast = i + 1 == fields.length;
      var field = fields[i];

      if (isLast) {
        map.put(field, val);
      } else {
        if (!map.containsKey(field)) {
          map.put(field, new HashMap<>());
        }
        map = (Map) map.get(field);
      }
    }
  }

  private static Object getField(Map<String, Object> model, String getField) {
    var fields = getField.split("\\.");
    if (fields.length == 0) {
      throw new IllegalArgumentException("Invalid size");
    }

    for (int i = 0; i < fields.length - 1; i++) {
      var field = fields[i];
      model = (Map<String, Object>) model.get(field);
    }

    var lastField = fields[fields.length - 1];
    return model.get(lastField);
  }
}
