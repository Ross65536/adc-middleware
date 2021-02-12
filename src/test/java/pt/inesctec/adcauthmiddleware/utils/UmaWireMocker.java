package pt.inesctec.adcauthmiddleware.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.base.Charsets;
import org.springframework.http.HttpHeaders;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.resources.RepertoireResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UmaWireMocker {

  private static String UMA_PERMISSION_PATH = "/auth/permission";
  private static String UMA_INTROSPECTION_PATH = "/auth/introspection";
  private static String UMA_TOKEN_PATH = "/auth/token";
  public static String UMA_RESOURCE_REGISTRATION_PATH = "/auth/registration";
  private static String UMA_ISSUER_PATH = "/auth";

  public static void wireUmaWellKnown(WireMockServer umaMock) {
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

  public static void wireListResources(WireMockServer umaMock, String expectedBearer, String ... umaIds) {

    WireMocker.wireGetJson(umaMock, UMA_RESOURCE_REGISTRATION_PATH, 200, List.of(umaIds), "Bearer " + expectedBearer);
  }

  public static String wireTokenEndpoint(WireMockServer umaMock) {
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

  public static String wireCreateResource(WireMockServer umaMock, Map<String, Object> repertoire, String expectedBearer) {
    var studyId = TestCollections.getString(repertoire, RepertoireResource.UMA_ID_FIELD);
    var studyTitle = TestCollections.getString(repertoire, RepertoireResource.STUDY_TITLE_FIELD);
    var name = String.format("study ID: %s; title: %s", studyId, studyTitle);
    var createdId = studyId + "-" + TestConstants.Random.nextInt(100);

    var expected = TestCollections.mapOf(
        Pair.of("name", name),
        Pair.of("type", AdcConstants.UMA_STUDY_TYPE),
        Pair.of("owner", TestConstants.UMA_RESOURCE_OWNER),
        Pair.of("ownerManagedAccess", true),
        Pair.of("resource_scopes", TestConstants.UMA_ALL_SCOPES)
    );

    var response = Map.of(
        "_id", createdId
    );

    WireMocker.wirePostJson(umaMock, UMA_RESOURCE_REGISTRATION_PATH, 200, response, expected, "Bearer " + expectedBearer);

    return createdId;
  }

  public static String wireGetTicket(WireMockServer umaMock, String expectedBearer, Map<String, Object> ... resources) {
    var ticket = TestConstants.generateHexString(30);

    var response = Map.of("ticket", ticket);

    WireMocker.wirePostJson(umaMock, UMA_PERMISSION_PATH, 200, response, List.of(resources), "Bearer " + expectedBearer);

    return ticket;
  }

  public static void wireSyncIntrospection(WireMockServer umaMock, String accessToken) {
    var realm_access = Map.of(
        "roles", Set.of(TestConstants.SYNC_ROLE)
    );

    var response = Map.of(
        "active", true,
        "realm_access", realm_access
    );

    var rptToken = TestConstants.generateHexString(30);
    var expectedForm = Map.of(
        "token", accessToken,
        "token_type_hint", "access_token"
    );

    var basic = HttpHeaders.encodeBasicAuth(TestConstants.UMA_CLIENT_ID, TestConstants.UMA_CLIENT_SECRET, Charsets.UTF_8); // keycloak specific inadequacy
    WireMocker.wireExpectFormReturnJson(umaMock, UMA_INTROSPECTION_PATH, 200, response, expectedForm, "Basic " + basic);
  }

  public static String wireTokenIntrospection(WireMockServer umaMock, Map<String, Object> ... resources) {
    var response = Map.of(
        "active", true,
        "permissions", resources
    );

    var rptToken = TestConstants.generateHexString(30);
    var expectedForm = Map.of(
        "token", rptToken,
        "token_type_hint", "requesting_party_token"
    );

    var basic = HttpHeaders.encodeBasicAuth(TestConstants.UMA_CLIENT_ID, TestConstants.UMA_CLIENT_SECRET, Charsets.UTF_8); // keycloak specific inadequacy
    WireMocker.wireExpectFormReturnJson(umaMock, UMA_INTROSPECTION_PATH, 200, response, expectedForm, "Basic " + basic);

    return rptToken;
  }

  public static String wireTokenIntrospectionExpired(WireMockServer umaMock) {
    var response = Map.of(
        "active", false
    );

    var rptToken = TestConstants.generateHexString(30);
    var expectedForm = Map.of(
        "token", rptToken,
        "token_type_hint", "requesting_party_token"
    );

    var basic = HttpHeaders.encodeBasicAuth(TestConstants.UMA_CLIENT_ID, TestConstants.UMA_CLIENT_SECRET, Charsets.UTF_8); // keycloak specific inadequacy
    WireMocker.wireExpectFormReturnJson(umaMock, UMA_INTROSPECTION_PATH, 200, response, expectedForm, "Basic " + basic);

    return rptToken;
  }

  private static String buildBaseUrl(WireMockServer umaMock) {
    return "http://localhost:" + umaMock.getOptions().portNumber();
  }

  public static void wireGetResource(WireMockServer umaMock, String umaId, String name, List<String> umaScopes, String expectedBearer) {

    var response =
        Map.of(
            "name",
            name,
            "resource_scopes",
            umaScopes.stream().map(e -> Map.of("name", e)).collect(Collectors.toList()));

    WireMocker.wireGetJson(umaMock, UMA_RESOURCE_REGISTRATION_PATH + "/" + umaId, 200, response, "Bearer " + expectedBearer);
  }

  public static void wirePutResource(WireMockServer umaMock, String umaId, String name, List<String> umaScopes, String expectedBearer, String umaResourceType) {
    var expectedRequest = new HashMap<String, Object>();
    expectedRequest.put("name", name);
    if (umaScopes != null) {
      expectedRequest.put("resource_scopes", umaScopes);
    }
    expectedRequest.put("type", umaResourceType);

    WireMocker.wirePutJson(umaMock, UMA_RESOURCE_REGISTRATION_PATH + "/" + umaId, 200, null, expectedRequest,"Bearer " + expectedBearer);
  }

  public static void wireDeleteResource(WireMockServer umaMock, String umaId, String expectedBearer) {
    WireMocker.wireDelete(umaMock, UMA_RESOURCE_REGISTRATION_PATH + "/" + umaId, 200, "Bearer " + expectedBearer);
  }
}
