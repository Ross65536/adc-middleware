package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdcPublicEndpointTests {
  private static final int BACKEND_PORT = 8883;
  @ClassRule
  private static WireMockServer backendMock = new WireMockRule(options().port(BACKEND_PORT));
  private static ObjectMapper JsonObjectMapper = new ObjectMapper();

  @LocalServerPort
  private int port;
  @Autowired
  private TestRestTemplate restTemplate;


  private static String toJson(Object obj) throws JsonProcessingException {
    return JsonObjectMapper.writeValueAsString(obj);
  }

  private static String JSON_MIME = "application/json";

  public static void setupGetJsonMock(WireMockServer mock, String url, int status, String json) {
    mock.stubFor(WireMock.get(url)
        .withHeader("Accept",  containing(JSON_MIME))
        .willReturn(WireMock.aResponse()
            .withStatus(status)
            .withBody(json)
            .withHeader("Content-Type", JSON_MIME)
        )
    );
  }

  public static void setupGetJsonMock(WireMockServer mock, String url, int status, Object body) throws JsonProcessingException {
    setupGetJsonMock(mock, url, status, toJson(body));
  }

  public Map<String, Object> getJsonObj(String path, int expectedStatus) throws JsonProcessingException {
    var entity = this.restTemplate.getForEntity(path, String.class);
    assertThat(entity.getStatusCodeValue()).isEqualTo(expectedStatus);
    return JsonObjectMapper.readValue(entity.getBody(), Map.class);
  }

  @Test
  void rootOk() throws JsonProcessingException {
    int status = 200;
    String path = "/airr/v1";
    var info = TestMaps.of(
        Pair.of("result", "success")
    );

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = getJsonObj("http://localhost:" + port + path + "/", status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void infoOk() throws JsonProcessingException {
    int status = 200;
    String path = "/airr/v1/info";
    var info = TestMaps.of(
        Pair.of("name", "airr"),
        Pair.of("last_update", null)
    );

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = getJsonObj("http://localhost:" + port + path, status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void infoError() throws JsonProcessingException {
    int status = 401;
    String path = "/airr/v1/info";
    var info = TestMaps.of(
        Pair.of("result", "error")
    );

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = getJsonObj("http://localhost:" + port + path + "/", status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void swaggerOk() throws JsonProcessingException {
    // not sure what swagger is suppoed to return
    int status = 200;
    String path = "/airr/v1/swagger";
    var info = TestMaps.of(
        Pair.of("result", "success")
    );

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = getJsonObj("http://localhost:" + port + path + "/", status);
    assertThat(actualInfo).isEqualTo(info);
  }

}
