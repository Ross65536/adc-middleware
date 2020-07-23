package pt.inesctec.adcauthmiddleware.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides utility methods for processing JSON.
 */
public final class Json {
  public static ObjectMapper JsonObjectMapper = new ObjectMapper();

  /**
   * Create JSON from object.
   *
   * @param body the source object
   * @return JSON string.
   * @throws JsonProcessingException on encode error
   */
  public static String toJson(Object body) throws JsonProcessingException {
    return JsonObjectMapper.writeValueAsString(body);
  }

  /**
   * Parse JSON into model.
   *
   * @param respClass target model
   * @param inputStream source byte JSON stream.
   * @param <T> target model class.
   * @return the model
   * @throws IOException on error
   */
  static <T> T parseJson(Class<T> respClass, InputStream inputStream) throws IOException {
    return JsonObjectMapper.readValue(inputStream, respClass);
  }
}
