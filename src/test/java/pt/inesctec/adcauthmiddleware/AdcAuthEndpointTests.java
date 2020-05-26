package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.HashMap;
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

// cache must be disabled for these tests
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdcAuthEndpointTests extends TestBase {

  private static final Set<String> RepertoireIdFields = Set.of(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);
  private static final Set<String> RearrangementIdFields = Set.of(AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);
  private static WireMockServer umaMock = new WireMockRule(options().port(TestConstants.UMA_PORT));

  private Map<String, Object> firstRepertoire;
  private Map<String, Object> secondRepertoire;
  private String firstRepertoireUmaId;
  private String secondRepertoireUmaId;
  private String accessToken;

  private static final Set<String> RepertoirePublicFields = Set.of(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD);


  @BeforeAll
  public void init() throws JsonProcessingException {

    var searchRequest =
        ModelFactory.buildAdcFields(
            AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD,
            AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
            AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD);

    this.firstRepertoire = ModelFactory.buildRepertoire("1");
    this.secondRepertoire = ModelFactory.buildRepertoire("2");
    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(firstRepertoire, secondRepertoire);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, searchRequest);
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
  public void singleRepertoireMismatchedRptToken() throws JsonProcessingException {
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
                this.secondRepertoireUmaId,
                TestConstants.UMA_SCOPES)); // access token for different repertoire provided
    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
            200,
            token);

    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(
        TestCollections.mapSubset(this.firstRepertoire, RepertoirePublicFields)
    ));
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
  public void adcSearchInputValidation() {
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

    checker.accept(400, TestJson.toJson(Map.of("filters", Map.of("op", "zxY"))));
    checker.accept(
        400, TestJson.toJson(Map.of("filters", Map.of("op", "=", "content", List.of()))));

    checker.accept(
        422,
        TestJson.toJson(
            Map.of(
                "fields",
                List.of(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD),
                "facets",
                AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD)));

    checker.accept(
        422,
        TestJson.toJson(
            Map.of(
                "filters",
                Map.of(
                    "op",
                    "=",
                    "content",
                    Map.of(
                        "field", "nont existent field xYZ",
                        "value", "1")))));

    checker.accept(
        422,
        TestJson.toJson(
            Map.of(
                "filters",
                Map.of(
                    "op",
                    "=",
                    "content",
                    Map.of(
                        "field", AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD, "value", false)))));
  }

  @Test
  public void repertoireSearchTicketAll() throws JsonProcessingException {
    var repertoireIdFields = Set.of(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);

    var request = ModelFactory.buildAdcFilters(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var ticketRequest =
        TestCollections.mapMerge(request, ModelFactory.buildAdcFields(repertoireIdFields));

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(
            TestCollections.mapSubset(firstRepertoire, repertoireIdFields),
            TestCollections.mapSubset(secondRepertoire, repertoireIdFields));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId,
                TestConstants.UMA_SCOPES)); // repertoires have all the scopes

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchTicketSingle() throws JsonProcessingException {

    var request = ModelFactory.buildAdcFilters(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var ticketRequest =
        TestCollections.mapMerge(request, ModelFactory.buildAdcFields(RepertoireIdFields));

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(
            TestCollections.mapSubset(firstRepertoire, RepertoireIdFields));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                TestConstants.UMA_SCOPES)); // repertoires have all the scopes

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchTicketScopeLimit() throws JsonProcessingException {
    // based on fields limits to 'raw_sequence' scope
    var request = ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    var ticketRequest = ModelFactory.buildAdcFields(RepertoireIdFields);

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(
            TestCollections.mapSubset(firstRepertoire, RepertoireIdFields));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireFacetsTicket() throws JsonProcessingException {
    // based on facets limits to 'raw_sequence' scope
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    var ticketRequest = ModelFactory.buildAdcFields(RepertoireIdFields);

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(
            TestCollections.mapSubset(firstRepertoire, RepertoireIdFields));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void rearrangementsFacetsTicket() throws JsonProcessingException {
    // based on facets limits to 'raw_sequence' scope
    var repertoireId = TestCollections.getString(this.firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var request = ModelFactory.buildAdcFacets("sequence_aa");

    var rearrangement = ModelFactory.buildRearrangement(repertoireId, "1");
    var ticketRequest = ModelFactory.buildAdcFields(RearrangementIdFields);
    var repertoiresResponse = ModelFactory.buildRearrangementsDocumentWithInfo(
            TestCollections.mapSubset(rearrangement, RearrangementIdFields));
    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchPublic() throws JsonProcessingException {
    Set<String> fields = Set.of(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_ID_FIELD);
    var request = ModelFactory.buildAdcFields(fields);

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            TestJson.toJson(request),
            200);

    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(
        TestCollections.mapSubset(this.firstRepertoire, fields),
        TestCollections.mapSubset(this.secondRepertoire, fields)
    ));
  }

  @Test
  public void repertoireSearchAllAccess() throws JsonProcessingException {
    var request = Map.of();

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_SCOPES),
            ModelFactory.buildUmaResource(this.secondRepertoireUmaId, TestConstants.UMA_SCOPES)
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire));
  }

  @Test
  public void repertoireSearchAllAccessFullAdcQueryFieldFilterWithOversizedRpt() throws JsonProcessingException {
    var fields = Set.of(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    Map<String, Object> queryExtras = Map.of(
    "from", 1,
    "size", 1,
    "format", "json"
    );

    Map<String, Object> queryFilters = ModelFactory.buildAdcFilters(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var request = TestCollections.mapMerge(
        ModelFactory.buildAdcFields(fields),
        queryFilters,
        queryExtras
    );

    var backendRequest = TestCollections.mapMerge(
        ModelFactory.buildAdcFields(Set.of(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD, AdcConstants.REPERTOIRE_STUDY_ID_FIELD)),
        queryFilters,
        queryExtras
    );

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)),
            ModelFactory.buildUmaResource(this.secondRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE))
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(
        TestCollections.mapSubset(this.firstRepertoire, fields)
    ));
  }

  @Test
  public void repertoireSearchUnknownResponseFields() throws JsonProcessingException {
    var request = Map.of();

    var backendRepertoire = new HashMap<>(this.firstRepertoire);
    backendRepertoire.put("field-xyz-unknown", 1); // would fail equality assert below if piped to requester
    var repertoiresResponse = ModelFactory.buildRepertoiresDocumentWithInfo(backendRepertoire);
    WireMocker.wirePostJson(backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token = UmaWireMocker.wireTokenIntrospection(
        umaMock, ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_SCOPES)
    );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire));
  }

  @Test
  public void repertoireSearchPartialAccessDeny() throws JsonProcessingException {
    var request = Map.of();

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE))
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(
        TestCollections.mapSubset(this.firstRepertoire, Set.of("study", "data_processing.data_processing_files", "repertoire_id")),
        TestCollections.mapSubset(this.secondRepertoire, RepertoirePublicFields)
    ));
  }

  @Test
  public void rearrangementSearchAllAccess() throws JsonProcessingException {
    var repertoireId1 = TestCollections.getString(this.firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var repertoireId2 = TestCollections.getString(this.secondRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId1, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId2, "2");
    var rearrangementResponse = ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_SCOPES),
            ModelFactory.buildUmaResource(this.secondRepertoireUmaId, TestConstants.UMA_SCOPES)
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2));
  }

  @Test
  public void rearrangementSearchAllAccessSameRepertoire() throws JsonProcessingException {
    var repertoireId = TestCollections.getString(this.firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId, "2");
    var rearrangementResponse = ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE))
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2));
  }

  @Test
  public void rearrangementSearchPartialAccessDeny() throws JsonProcessingException {
    var repertoireId1 = TestCollections.getString(this.firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var repertoireId2 = TestCollections.getString(this.secondRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId1, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId2, "2");
    var rearrangementResponse = ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)),
            ModelFactory.buildUmaResource(this.secondRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE)) // should deny
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1));
  }

  @Test
  public void rearrangementSearchPartialFullAccessDeny() throws JsonProcessingException {
    var repertoireId1 = TestCollections.getString(this.firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var repertoireId2 = TestCollections.getString(this.secondRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId1, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId2, "2");
    var rearrangementResponse = ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo());
  }

  @Test
  public void repertoireFacetsAllAccess() throws JsonProcessingException {
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    var facet = ModelFactory.buildFacet(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);

    var backendRequest = TestCollections.mapMerge(
      request,
      ModelFactory.buildAdcFacetsFilter(AdcConstants.REPERTOIRE_STUDY_ID_FIELD, List.of(
          TestCollections.getString(this.firstRepertoire, AdcConstants.REPERTOIRE_STUDY_ID_FIELD),
          TestCollections.getString(this.secondRepertoire, AdcConstants.REPERTOIRE_STUDY_ID_FIELD)
      ))
    );

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_SCOPES),
            ModelFactory.buildUmaResource(this.secondRepertoireUmaId, TestConstants.UMA_SCOPES)
        );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void repertoireFacetsDenyAccess() throws JsonProcessingException {
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    List<Map<String, Object>> facet = List.of();

    var backendRequest = TestCollections.mapMerge(
        request,
        ModelFactory.buildAdcFacetsFilter(AdcConstants.REPERTOIRE_STUDY_ID_FIELD, List.of())
    );

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token = UmaWireMocker.wireTokenIntrospection(umaMock);

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            request,
            200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void repertoireFacetsDenyAccessWhitelisting() throws JsonProcessingException {
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    List<Map<String, Object>> facet = ModelFactory.buildFacet(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);

    var backendRequest = TestCollections.mapMerge(
        request,
        ModelFactory.buildAdcFacetsFilter(AdcConstants.REPERTOIRE_STUDY_ID_FIELD, List.of()) // for access denied, an empty 'in' is sent
    );

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token = UmaWireMocker.wireTokenIntrospection(umaMock);

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
            request,
            200, token);

    List<Map<String, Object>>  expectedFacet = List.of();
    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(expectedFacet));
  }


}
