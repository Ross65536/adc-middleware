package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.utils.Pair;
import pt.inesctec.adcauthmiddleware.utils.TestCollections;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static pt.inesctec.adcauthmiddleware.utils.WireMocker.wireGetJson;

class AdcPublicEndpointTests extends TestBase {


  @BeforeEach
  void reset() {
    backendMock.resetAll();
  }

  @Test
  void rootOk() throws JsonProcessingException {
    int status = 200;
    var info = TestCollections.mapOf(Pair.of("result", "success"));

    wireGetJson(backendMock, TestConstants.BASE_MIDDLEWARE_PATH, status, info);
    backendMock.start();

    var actualInfo = requests.getJsonMap(buildMiddlewareUrl() + "/", status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void infoOk() throws JsonProcessingException {
    int status = 200;
    String path = "info";
    var info = TestCollections.mapOf(Pair.of("name", "airr"), Pair.of("last_update", null));

    wireGetJson(backendMock, TestConstants.buildAirrPath(path), status, info);
    backendMock.start();

    var actualInfo = requests.getJsonMap(buildMiddlewareUrl(path), status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void infoError() throws JsonProcessingException {
    int status = 401;
    String path = "info";
    var info = TestCollections.mapOf(Pair.of("result", "error"));

    wireGetJson(backendMock, TestConstants.buildAirrPath(path), status, info);
    backendMock.start();

    var actualInfo = requests.getJsonMap(buildMiddlewareUrl(path), status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void swaggerOk() throws JsonProcessingException {
    // not sure what swagger is suppoed to return
    int status = 200;
    String path = "swagger";
    var info = TestCollections.mapOf(Pair.of("result", "success"));

    wireGetJson(backendMock, TestConstants.buildAirrPath(path), status, info);
    backendMock.start();

    var actualInfo = requests.getJsonMap(buildMiddlewareUrl(path), status);
    assertThat(actualInfo).isEqualTo(info);
  }

  @Test
  void publicFields() throws JsonProcessingException {
    // based on file src/test/resources/field-mapping.csv
    String[] expectedFields = new String[]{AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD};

    var actualFields = requests.getJsonMap(buildMiddlewareUrl("public_fields"), 200);
    assertThat(actualFields).containsOnlyKeys("Repertoire");
    assertThat((List<String>) actualFields.get("Repertoire")).containsExactlyInAnyOrder(expectedFields);
  }
}
