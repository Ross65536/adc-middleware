package pt.inesctec.adcauthmiddleware.utils;

import java.util.List;
import java.util.Random;
/**
 * These constants should match the values in file 'src/test/resources/application.properties', 'src/test/resources/field-mapping.csv'
 */
public final class TestConstants {
  public static final Random Random = new Random();

  public static final int BACKEND_PORT = 8883;
  public static final int UMA_PORT = 8882;
  public static final String BASE_MIDDLEWARE_PATH = "/airr/v1";
  public static final String SYNC_PASSWORD = "master";

  public static final String UMA_WELL_KNOWN_PATH = "/.well-known/uma2-configuration";
  public static final String UMA_CLIENT_ID="adc-middleware";
  public static final String UMA_CLIENT_SECRET="d2f67b7d-3d87-43ac-a9e1-f9dc462c0c0f";
  public static final String UMA_RESOURCE_OWNER="owner";

  public static final List<String> UMA_SCOPES = List.of("statistics", "raw_sequence");


  public static String buildAirrPath(String path) {
    return BASE_MIDDLEWARE_PATH + "/" + path;
  }

  // from https://stackoverflow.com/a/14623245/6711421
  public static String generateHexString(int numChars) {
    StringBuffer buffer = new StringBuffer();
    while(buffer.length() < numChars) {
      buffer.append(String.format("%08x", Random.nextInt()));
    }

    return buffer.toString().substring(0, numChars);
  }
}
