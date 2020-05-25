package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.utils.ModelFactory;
import pt.inesctec.adcauthmiddleware.utils.TestCollections;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;
import pt.inesctec.adcauthmiddleware.utils.TestJson;
import pt.inesctec.adcauthmiddleware.utils.UmaWireMocker;
import pt.inesctec.adcauthmiddleware.utils.WireMocker;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdcAuthEndpointTests extends TestBase {

  private static WireMockServer umaMock = new WireMockRule(options().port(TestConstants.UMA_PORT));

  private Map<String, Object> firstRepertoire;
  private Map<String, Object> secondRepertoire;
  private String firstRepertoireUmaId;
  private String secondRepertoireUmaId;
  private String accessToken;

  @BeforeAll
  public void init() throws JsonProcessingException {

    var searchRequest =
        ModelFactory.buildAdcSearch(
            AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD,
            AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
            AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD);

    this.firstRepertoire = ModelFactory.buildRepertoire("1");
    this.secondRepertoire = ModelFactory.buildRepertoire("2");
    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(firstRepertoire, secondRepertoire);

    WireMocker.wirePostJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        200,
        repertoiresResponse,
        searchRequest);
    backendMock.start();

    UmaWireMocker.wireUmaWellKnown(umaMock);
    this.accessToken = UmaWireMocker.wireTokenEndpoint(umaMock);
    UmaWireMocker.wireListResources(umaMock, accessToken);
    this.firstRepertoireUmaId =
        UmaWireMocker.wireCreateResource(umaMock, firstRepertoire, accessToken);
    this.secondRepertoireUmaId =
        UmaWireMocker.wireCreateResource(umaMock, secondRepertoire, accessToken);
    umaMock.start();

    this.requests.postEmpty(
        this.buildMiddlewareUrl(TestConstants.SYNCHRONIZE_PATH_FRAGMENT),
        TestConstants.SYNC_PASSWORD,
        200);
  }

  @BeforeEach
  public void reset() throws JsonProcessingException {
    backendMock.resetMappings();
    umaMock.resetMappings();
    this.accessToken = UmaWireMocker.wireTokenEndpoint(umaMock);
  }

  @Test
  public void synchronize() {
    // empty because the synchronization is done in init()
    // if tests fail check logs that resources are created, since they can fail to create without
    // failing the synchronize
  }

  @Test
  public void singleRepertoireTicket() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                TestConstants.UMA_SCOPES)); // repertoires have all the scopes

    this.requests.getJsonUmaTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), ticket);
  }

  @Test
  public void notFoundSingleRepertoire() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD)
            + "2324";

    this.requests.getJsonMap(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), 404);
  }

  @Test
  public void singleRepertoireAllAccess() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);

    WireMocker.wireGetJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
        200,
        ModelFactory.buildRepertoiresDocumentWithInfo(firstRepertoire));

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                TestConstants.UMA_SCOPES)); // repertoires have all the scopes
    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
            200,
            token);

    assertThat(actual)
        .isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire));
  }

  @Test
  public void singleRepertoireExpiredRptToken() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);

    WireMocker.wireGetJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
        200,
        ModelFactory.buildRepertoiresDocumentWithInfo(
            firstRepertoire)); // add mock for returning the resource to check that the resource is
                               // not actually returned

    var token = UmaWireMocker.wireTokenIntrospectionExpired(umaMock);
    this.requests.getJsonMap(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), 401, token);
  }

  @Test
  public void singleRepertoireOneScopeFiltering() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);

    WireMocker.wireGetJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
        200,
        ModelFactory.buildRepertoiresDocumentWithInfo(firstRepertoire));

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, List.of(TestConstants.UMA_STATISTICS_SCOPE)));
    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
            200,
            token);

    var expected =
        TestCollections.mapSubset(
            this.firstRepertoire,
            Set.of("repertoire_id", "study", "data_processing.data_processing_files"));
    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(expected));
  }

  @Test
  public void singleRepertoirePublicFiltering() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);

    WireMocker.wireGetJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
        200,
        ModelFactory.buildRepertoiresDocumentWithInfo(firstRepertoire));

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock, ModelFactory.buildUmaResource(this.firstRepertoireUmaId, List.of()));
    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
            200,
            token);

    var expected =
        TestCollections.mapSubset(
            this.firstRepertoire, Set.of("repertoire_id", "study.study_id", "study.study_title"));
    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(expected));
  }

  @Test
  public void singleRearrangementTicket() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    String rearrangementId = "1";
    var rearrangement = ModelFactory.buildRearrangement(repertoireId, rearrangementId);

    WireMocker.wireGetJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REARRANGEMENT_PATH_FRAGMENT, rearrangementId),
        200,
        ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement));

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, List.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.getJsonUmaTicket(
        this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT, rearrangementId),
        ticket);
  }

  @Test
  public void singleRearrangementAllAccess() throws JsonProcessingException {
    var repertoireId =
        TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    String rearrangementId = "1";
    var rearrangement = ModelFactory.buildRearrangement(repertoireId, rearrangementId);

    WireMocker.wireGetJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REARRANGEMENT_PATH_FRAGMENT, rearrangementId),
        200,
        ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement));

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, List.of(TestConstants.UMA_SEQUENCE_SCOPE)));
    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT, rearrangementId),
            200,
            token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement));
  }

  @Test
  public void notFoundSingleRearrangement() throws JsonProcessingException {
    String rearrangementId = "1";

    WireMocker.wireGetJson(
        backendMock,
        TestConstants.buildAirrPath(TestConstants.REARRANGEMENT_PATH_FRAGMENT, rearrangementId),
        200,
        ModelFactory.buildRearrangementsDocumentWithInfo());

    this.requests.getJsonMap(
        this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT, rearrangementId), 404);
  }

  @Test
  public void adcSearchSchemaValidation() {
    BiConsumer<Integer, String> checker =
        (status, json) -> {
          this.requests.postJson(
              this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), json, status);
          this.requests.postJson(
              this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT), json, status);
        };

    checker.accept(400, "/.dw");
    checker.accept(400, "[1]");
    checker.accept(400, "{\"a\":1}");
    checker.accept(400, "{\"fields\":1}");
    checker.accept(400, "{\"fields\":\"repertoire_id\"}");

    checker.accept(400, TestJson.toJson(Map.of(
        "filters", Map.of(
            "op", "zxY"
        )
    )));
    checker.accept(400, TestJson.toJson(Map.of(
        "filters", Map.of(
            "op", "=",
            "content", List.of()
        )
    )));

    checker.accept(422, TestJson.toJson(Map.of(
        "fields", List.of(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD),
        "facets", AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD
    )));

    checker.accept(422, TestJson.toJson(Map.of(
        "filters", Map.of(
            "op", "=",
            "content", Map.of(
                "field", "nont existent field xYZ",
                "value", "1"
            )
        )
    )));

    checker.accept(422, TestJson.toJson(Map.of(
        "filters", Map.of(
            "op", "=",
            "content", Map.of(
                "field", AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD,
                "value", false
            )
        )
    )));
  }
}
