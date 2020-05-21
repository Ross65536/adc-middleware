package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static pt.inesctec.adcauthmiddleware.utils.TestJson.toJson;

public final class WireMocker {
  private static final String JSON_MIME = "application/json";


  public static void setupGetJsonMock(WireMockServer mock, String url, int status, String json) {
    mock.stubFor(
        WireMock.get(url)
            .withHeader("Accept", containing(JSON_MIME))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(status)
                    .withBody(json)
                    .withHeader("Content-Type", JSON_MIME)));
  }

  public static void setupGetJsonMock(WireMockServer mock, String url, int status, Object body)
      throws JsonProcessingException {
    setupGetJsonMock(mock, url, status, toJson(body));
  }
}
