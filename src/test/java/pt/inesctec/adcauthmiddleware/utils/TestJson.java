package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class TestJson {
  private static ObjectMapper JsonObjectMapper = new ObjectMapper();

  public static String toJson(Object obj) {
    try {
      return JsonObjectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T fromJson(String body, Class<T> clazz) {
    try {
      return JsonObjectMapper.readValue(body, clazz);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
