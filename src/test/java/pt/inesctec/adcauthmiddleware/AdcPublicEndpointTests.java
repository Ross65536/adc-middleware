package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import pt.inesctec.adcauthmiddleware.utils.Pair;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;
import pt.inesctec.adcauthmiddleware.utils.TestMaps;
import pt.inesctec.adcauthmiddleware.utils.Requests;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static pt.inesctec.adcauthmiddleware.utils.WireMocker.setupGetJsonMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdcPublicEndpointTests {

  @ClassRule
  private static WireMockServer backendMock = new WireMockRule(options().port(TestConstants.BACKEND_PORT));

  @LocalServerPort private int port;
  @Autowired private Requests requests;

  @Before
  void reset() {
    backendMock.resetAll();
  }

  @Test
  void rootOk() throws JsonProcessingException {
    int status = 200;
    String path = "/airr/v1";
    var info = TestMaps.of(Pair.of("result", "success"));

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = requests.getJsonObj("http://localhost:" + port + path + "/", status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void infoOk() throws JsonProcessingException {
    int status = 200;
    String path = "/airr/v1/info";
    var info = TestMaps.of(Pair.of("name", "airr"), Pair.of("last_update", null));

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = requests.getJsonObj("http://localhost:" + port + path, status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void infoError() throws JsonProcessingException {
    int status = 401;
    String path = "/airr/v1/info";
    var info = TestMaps.of(Pair.of("result", "error"));

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = requests.getJsonObj("http://localhost:" + port + path + "/", status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void swaggerOk() throws JsonProcessingException {
    // not sure what swagger is suppoed to return
    int status = 200;
    String path = "/airr/v1/swagger";
    var info = TestMaps.of(Pair.of("result", "success"));

    setupGetJsonMock(backendMock, path, status, info);
    backendMock.start();

    var actualInfo = requests.getJsonObj("http://localhost:" + port + path + "/", status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void publicFields() throws JsonProcessingException {
    // based on file src/test/resources/field-mapping.csv
    String[] expectedFields = new String[]{"repertoire_id", "study.study_id", "study.study_title"};

    var actualFields = requests.getJsonObj("http://localhost:" + port + "/airr/v1/public_fields", 200);
    assertThat(actualFields).containsOnlyKeys("Repertoire");
    assertThat((List<String>) actualFields.get("Repertoire")).containsExactlyInAnyOrder(expectedFields);
  }
}
