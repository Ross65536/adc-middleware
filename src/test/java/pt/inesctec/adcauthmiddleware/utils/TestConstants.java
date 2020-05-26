package pt.inesctec.adcauthmiddleware.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
/**
 * These constants should match the values in file 'src/test/resources/application.properties',
 * 'src/test/resources/field-mapping.csv'
 */
public final class TestConstants {
  public static final Random Random = new Random();

  public static final int BACKEND_PORT = 8883;
  public static final int UMA_PORT = 8882;
  public static final String BASE_MIDDLEWARE_PATH = "/airr/v1";
  public static final String SYNC_PASSWORD = "master";

  public static final String UMA_WELL_KNOWN_PATH = "/.well-known/uma2-configuration";
  public static final String UMA_CLIENT_ID = "adc-middleware";
  public static final String UMA_CLIENT_SECRET = "d2f67b7d-3d87-43ac-a9e1-f9dc462c0c0f";
  public static final String UMA_RESOURCE_OWNER = "owner";

  public static final String UMA_STATISTICS_SCOPE = "statistics";
  public static final String UMA_SEQUENCE_SCOPE = "raw_sequence";
  public static final List<String> UMA_SCOPES = List.of(UMA_STATISTICS_SCOPE, UMA_SEQUENCE_SCOPE);
  public static final String REPERTOIRE_PATH_FRAGMENT = "repertoire";
  public static final String REARRANGEMENT_PATH_FRAGMENT = "rearrangement";
  public static final String SYNCHRONIZE_PATH_FRAGMENT = "synchronize";
  public static final String REPERTOIRE_PATH = TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT);
  public static final String REARRANGEMENT_PATH = TestConstants.buildAirrPath(TestConstants.REARRANGEMENT_PATH_FRAGMENT);
  public static final String REPERTOIRE_PRIVATE_SEQUENCE_FIELD = "data_processing.numbo";
  public static final String REARRANGEMENT_PRIVATE_FIELD = "sequence_aa";

  public static String buildAirrPath(String... path) {
    var fullPath =
        Arrays.stream(path)
            .map(p -> URLEncoder.encode(p, StandardCharsets.UTF_8))
            .collect(Collectors.joining("/"));
    return BASE_MIDDLEWARE_PATH + "/" + fullPath;
  }

  // from https://stackoverflow.com/a/14623245/6711421
  public static String generateHexString(int numChars) {
    StringBuffer buffer = new StringBuffer();
    while (buffer.length() < numChars) {
      buffer.append(String.format("%08x", Random.nextInt()));
    }

    return buffer.toString().substring(0, numChars);
  }
}
