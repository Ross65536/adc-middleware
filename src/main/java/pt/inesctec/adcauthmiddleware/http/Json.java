package pt.inesctec.adcauthmiddleware.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public final class Json {
  public static ObjectMapper JsonObjectMapper = new ObjectMapper();

  public static String toJson(Object body) throws JsonProcessingException {
    return JsonObjectMapper.writeValueAsString(body);
  }

  static <T> T parseJson(Class<T> respClass, InputStream inputStream) throws IOException {
    return JsonObjectMapper.readValue(inputStream, respClass);
  }
}
