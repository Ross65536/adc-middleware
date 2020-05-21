package pt.inesctec.adcauthmiddleware.utils;

import java.util.HashMap;
import java.util.Map;

public final class TestMaps {
  public static  Map of(Pair... pairs) {
    var map = new HashMap();
    for (var pair: pairs) {
      map.put(pair.first, pair.second);
    }

    return map;
  }
}
