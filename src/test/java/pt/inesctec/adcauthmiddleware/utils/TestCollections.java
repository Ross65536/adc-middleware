package pt.inesctec.adcauthmiddleware.utils;

import java.util.HashMap;
import java.util.Map;

public final class TestCollections {
  public static <K,V> Map<K,V> mapOf(Pair<K,V>... pairs) {
    var map = new HashMap();
    for (var pair: pairs) {
      map.put(pair.first, pair.second);
    }

    return map;
  }

  public static String getString(Map<String, Object> model, String getField) {
    var fields = getField.split("\\.");
    if (fields.length == 0) {
      throw new IllegalArgumentException("Invalid size");
    }

    for (int i = 0; i < fields.length - 1; i++) {
      var field =  fields[i];
      model = (Map<String, Object>) model.get(field);

    }

    var lastField = fields[fields.length - 1];
    return (String) model.get(lastField);
  }
}
