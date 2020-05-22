package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.Map;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;

public class UmaWireMocker {

  private static String UMA_PERMISSION_PATH = "/auth/permission";
  private static String UMA_INTROSPECTION_PATH = "/auth/introspection";
  private static String UMA_TOKEN_PATH = "/auth/token";
  public static String UMA_RESOURCE_REGISTRATION_PATH = "/auth/registration";
  private static String UMA_ISSUER_PATH = "/auth";

  public static void wireUmaWellKnown(WireMockServer umaMock) throws JsonProcessingException {
    var baseUrl = buildBaseUrl(umaMock);
    var doc = Map.of(
        "permission_endpoint", baseUrl + UMA_PERMISSION_PATH,
        "introspection_endpoint", baseUrl + UMA_INTROSPECTION_PATH,
        "token_endpoint", baseUrl + UMA_TOKEN_PATH,
        "resource_registration_endpoint", baseUrl + UMA_RESOURCE_REGISTRATION_PATH,
        "issuer", baseUrl + UMA_ISSUER_PATH
    );

    WireMocker.wireGetJson( umaMock, TestConstants.UMA_WELL_KNOWN_PATH, 200, doc);
  }

  public static void wireListResources(WireMockServer umaMock, String expectedBearer) throws JsonProcessingException {

    WireMocker.wireGetJson(umaMock, UMA_RESOURCE_REGISTRATION_PATH, 200, List.of(), "Bearer " + expectedBearer);
  }

  public static String wireTokenEndpoint(WireMockServer umaMock) throws JsonProcessingException {
    var expectedForm = Map.of(
        "grant_type", "client_credentials",
        "client_id", TestConstants.UMA_CLIENT_ID,
        "client_secret", TestConstants.UMA_CLIENT_SECRET
    );

    String accessToken = TestConstants.generateHexString(16);
    var responseJson = Map.of(
        "access_token", accessToken
    );

    WireMocker.wireExpectFormReturnJson(umaMock, UMA_TOKEN_PATH, 200, responseJson, expectedForm);

    return accessToken;
  }

  public static String wireCreateResource(WireMockServer umaMock, Map<String, Object> repertoire, String bearer) throws JsonProcessingException {
    var studyId = TestCollections.getString(repertoire, "study", "study_id");
    var studyTitle = TestCollections.getString(repertoire, "study", "study_title");
    var name = String.format("study ID: %s; title: %s", studyId, studyTitle);
    var createdId = studyId + "-" + TestConstants.Random.nextInt(100);

    var expected = TestCollections.mapOf(
        Pair.of("name", name),
        Pair.of("type", AdcConstants.UMA_STUDY_TYPE),
        Pair.of("owner", TestConstants.UMA_RESOURCE_OWNER),
        Pair.of("ownerManagedAccess", true),
        Pair.of("resource_scopes", TestConstants.UMA_SCOPES)
    );

    var response = Map.of(
        "_id", createdId
    );

    WireMocker.wirePostJson(umaMock, UMA_RESOURCE_REGISTRATION_PATH, 200, response, expected, "Bearer " + bearer);

    return createdId;
  }

  private static String buildBaseUrl(WireMockServer umaMock) {
    return "http://localhost:" + umaMock.getOptions().portNumber();
  }
}
