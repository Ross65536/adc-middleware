package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class TestJson {
  private static ObjectMapper JsonObjectMapper = new ObjectMapper();

  public static String toJson(Object obj) throws JsonProcessingException {
    return JsonObjectMapper.writeValueAsString(obj);
  }

  public static <T> T fromJson(String body, Class<T> clazz) throws JsonProcessingException {
    return JsonObjectMapper.readValue(body, clazz);
  }
}
