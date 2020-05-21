package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.Map;
import org.junit.BeforeClass;
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
class AdcAuthMiddlewareApplicationTests {
  private static ObjectMapper JsonObjectMapper = new ObjectMapper();

  @LocalServerPort
  private int port;
  @Autowired
  private TestRestTemplate restTemplate;

  @ClassRule
  private static WireMockServer backend = new WireMockRule(options().port(8883));

  @BeforeClass
  public void init() {
  }

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

  @Test
  void publicInfo() throws JsonProcessingException {
    int status = 200;
    String path = "/airr/v1/info";
    var info = Map.of(
        "name", "airr",
        "last_update", 123
    );
    var respJson = toJson(info);

    setupGetJsonMock(backend, path, status, respJson);
    backend.start();

    var entity = this.restTemplate.getForEntity("http://localhost:" + port + path, String.class);
    assertThat(entity.getStatusCodeValue()).isEqualTo(status);
    assertThat(entity.getBody()).isEqualTo(respJson);

  }
}
