package pt.inesctec.adcauthmiddleware.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static pt.inesctec.adcauthmiddleware.utils.TestJson.toJson;

public final class WireMocker {
  public static final String JSON_MIME = "application/json";
  private static final String URL_ENCODED_MIME = "application/x-www-form-urlencoded";
  public static final String ACCEPT_HEADER = "Accept";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String AUTHORIZATION_HEADER = "Authorization";

  public static void wireGetJson(WireMockServer mock, String matchUrl, int respStatus, Object respBody) {
    mock.stubFor(
        WireMock.get(matchUrl)
            .withHeader(ACCEPT_HEADER, containing(JSON_MIME))
            .willReturn(jsonResponse(respStatus, respBody)));
  }

  public static void wireGetJson(WireMockServer mock, String matchUrl, int respStatus, Object respBody, String expectedAuthorization) {
    mock.stubFor(
        WireMock.get(matchUrl)
            .withHeader(ACCEPT_HEADER, containing(JSON_MIME))
            .withHeader(AUTHORIZATION_HEADER, equalTo(expectedAuthorization))
            .willReturn(jsonResponse(respStatus, respBody)));
  }

  public static void wirePostJson(
      WireMockServer mock,
      String matchUrl,
      int respStatus,
      Object responseBody,
      Object expectedBody) {
    var expectedJson = toJson(expectedBody);

    mock.stubFor(
        WireMock.post(matchUrl)
            .withHeader(CONTENT_TYPE_HEADER, containing(JSON_MIME))
            .withHeader(ACCEPT_HEADER, containing(JSON_MIME))
            .withRequestBody(equalToJson(expectedJson, true, false))
            .willReturn(jsonResponse(respStatus, responseBody)));
  }

  public static void wirePostJson(
      WireMockServer mock,
      String matchUrl,
      int respStatus,
      Object responseBody,
      Object expectedBody,
      String expectedAuthorization) {
    var expectedJson = toJson(expectedBody);

    mock.stubFor(
        WireMock.post(matchUrl)
            .withHeader(CONTENT_TYPE_HEADER, containing(JSON_MIME))
            .withHeader(ACCEPT_HEADER, containing(JSON_MIME))
            .withHeader(AUTHORIZATION_HEADER, equalTo(expectedAuthorization))
            .withRequestBody(equalToJson(expectedJson, true, false))
            .willReturn(jsonResponse(respStatus, responseBody)));
  }

  public static void wirePutJson(
      WireMockServer mock,
      String matchUrl,
      int respStatus,
      Object responseBody,
      Object expectedBody,
      String expectedAuthorization) {
    var expectedJson = toJson(expectedBody);

    mock.stubFor(
        WireMock.put(matchUrl)
            .withHeader(CONTENT_TYPE_HEADER, containing(JSON_MIME))
            .withHeader(ACCEPT_HEADER, containing(JSON_MIME))
            .withHeader(AUTHORIZATION_HEADER, equalTo(expectedAuthorization))
            .withRequestBody(equalToJson(expectedJson, true, false))
            .willReturn(jsonResponse(respStatus, responseBody)));
  }

  public static void wireExpectFormReturnJson(
      WireMockServer mock,
      String matchUrl,
      int respStatus,
      Object responseBody,
      Map<String, String> expectedBody) {

    MappingBuilder post =
        WireMock.post(matchUrl)
            .withHeader(CONTENT_TYPE_HEADER, containing(URL_ENCODED_MIME))
            .withHeader(ACCEPT_HEADER, containing(JSON_MIME));

    addFormEncodedChecks(expectedBody, post);

    mock.stubFor(post.willReturn(jsonResponse(respStatus, responseBody)));
  }

  public static void wireExpectFormReturnJson(
      WireMockServer mock,
      String matchUrl,
      int respStatus,
      Object responseBody,
      Map<String, String> expectedBody,
      String expectedAuthorization) {

    MappingBuilder post =
        WireMock.post(matchUrl)
            .withHeader(CONTENT_TYPE_HEADER, containing(URL_ENCODED_MIME))
            .withHeader(ACCEPT_HEADER, containing(JSON_MIME))
            .withHeader(AUTHORIZATION_HEADER, equalTo(expectedAuthorization));

    addFormEncodedChecks(expectedBody, post);

    mock.stubFor(post.willReturn(jsonResponse(respStatus, responseBody)));
  }

  public static void wireDelete(WireMockServer mock, String url, int returnStatus, String expectedAuthorization) {
    mock.stubFor(
        WireMock.delete(url)
            .withHeader(AUTHORIZATION_HEADER, equalTo(expectedAuthorization))
            .willReturn(WireMock.aResponse()
                .withStatus(returnStatus)));
  }

  private static void addFormEncodedChecks(Map<String, String> expectedBody, MappingBuilder post) {
    for (var entry : expectedBody.entrySet()) {
      String encodedPair =
          URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
              + "="
              + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
      post.withRequestBody(containing(encodedPair));
    }
  }

  private static ResponseDefinitionBuilder jsonResponse(int respStatus, Object response) {
    var json = toJson(response);

    return WireMock.aResponse()
        .withStatus(respStatus)
        .withBody(json)
        .withHeader(CONTENT_TYPE_HEADER, JSON_MIME);
  }
}
