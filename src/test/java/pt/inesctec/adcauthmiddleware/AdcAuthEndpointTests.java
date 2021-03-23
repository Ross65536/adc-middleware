package pt.inesctec.adcauthmiddleware;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import pt.inesctec.adcauthmiddleware.adc.RearrangementConstants;
import pt.inesctec.adcauthmiddleware.adc.RepertoireConstants;
import pt.inesctec.adcauthmiddleware.config.csv.IncludeField;
import pt.inesctec.adcauthmiddleware.utils.ModelFactory;
import pt.inesctec.adcauthmiddleware.utils.Pair;
import pt.inesctec.adcauthmiddleware.utils.TestCollections;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;
import pt.inesctec.adcauthmiddleware.utils.TestJson;
import pt.inesctec.adcauthmiddleware.utils.UmaWireMocker;
import pt.inesctec.adcauthmiddleware.utils.WireMocker;

// cache must be disabled for these tests
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdcAuthEndpointTests extends TestBase {

  private static final Set<String> RepertoireIdFields =
      Set.of(RepertoireConstants.UMA_ID_FIELD);
  private static final Set<String> RearrangementIdFields =
      Set.of(RearrangementConstants.REPERTOIRE_ID_FIELD);
  private static final WireMockServer umaMock =
      new WireMockRule(options().port(TestConstants.UMA_PORT));
  Set<String> RepertoireStatisticsScopeFields =
      Set.of(
          RearrangementConstants.REPERTOIRE_ID_FIELD,
          RepertoireConstants.STUDY_BASE,
          "data_processing.data_processing_files");

  private Map<String, Object> firstRepertoire;
  private Map<String, Object> secondRepertoire;
  private String firstRepertoireUmaId;
  private String secondRepertoireUmaId;
  private String accessToken;

  private static final Set<String> RepertoirePublicFields =
      Set.of(
          RepertoireConstants.ID_FIELD,
          RepertoireConstants.UMA_ID_FIELD,
          RepertoireConstants.STUDY_TITLE_FIELD);

  @BeforeAll
  public void init() {

    var searchRequest =
        ModelFactory.buildAdcFields(
            RepertoireConstants.ID_FIELD,
            RepertoireConstants.UMA_ID_FIELD,
            RepertoireConstants.STUDY_TITLE_FIELD);

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

    UmaWireMocker.wireSyncIntrospection(umaMock, this.accessToken);

    this.requests.postEmpty(
        this.buildMiddlewareUrl(TestConstants.SYNCHRONIZE_PATH_FRAGMENT),
        this.accessToken,
        200);
  }

  @AfterAll
  public static void stop() {
    umaMock.stop();
    backendMock.stop();
  }

  @BeforeEach
  public void reset() {
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
  public void singleRepertoireTicket() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                TestConstants.UMA_ALL_SCOPES)); // repertoires have all the scopes

    this.requests.getJsonUmaTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), ticket);
  }

  @Test
  public void notFoundSingleRepertoire() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD)
            + "2324";

    this.requests.getJsonMap(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), 404);
  }

  @Test
  public void singleRepertoireAllAccess() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);

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
                TestConstants.UMA_ALL_SCOPES)); // repertoires have all the scopes
    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
            200,
            token);

    assertThat(actual)
        .isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire));
  }

  @Test
  public void singleRepertoireExpiredRptToken() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);

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
  public void singleRepertoireMismatchedRptToken() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);

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
                TestConstants.UMA_ALL_SCOPES)); // access token for different repertoire provided
    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId),
            200,
            token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                TestCollections.mapSubset(this.firstRepertoire, RepertoirePublicFields)));
  }

  @Test
  public void singleRepertoireOneScopeFiltering() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);

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

    var expected = TestCollections.mapSubset(this.firstRepertoire, RepertoireStatisticsScopeFields);
    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(expected));
  }

  @Test
  public void singleRepertoirePublicFiltering() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);

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
            this.firstRepertoire,
            Set.of(
                RepertoireConstants.ID_FIELD,
                RepertoireConstants.UMA_ID_FIELD,
                RepertoireConstants.STUDY_TITLE_FIELD));
    assertThat(actual).isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(expected));
  }

  @Test
  public void singleRearrangementTicket() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);
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
  public void singleRearrangementAllAccess() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);
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
  public void singleRearrangementMismatchedRptToken() {
    var repertoireId =
        TestCollections.getString(firstRepertoire, RepertoireConstants.ID_FIELD);
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
                this.secondRepertoireUmaId, List.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    var actual =
        this.requests.getJsonMap(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT, rearrangementId),
            200,
            token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo());
  }

  @Test
  public void notFoundSingleRearrangement() {
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
    checker.accept(
        400, TestJson.toJson(Map.of("fields", RepertoireConstants.ID_FIELD)));

    checker.accept(400, TestJson.toJson(Map.of("filters", Map.of("op", "zxY"))));
    checker.accept(
        400, TestJson.toJson(Map.of("filters", Map.of("op", "=", "content", List.of()))));

    checker.accept(400, TestJson.toJson(Map.of("include_fields", "invalid_include12345")));

    checker.accept(
        422,
        TestJson.toJson(
            Map.of(
                "fields",
                List.of(RepertoireConstants.ID_FIELD),
                "facets",
                RepertoireConstants.ID_FIELD)));

    checker.accept(
        422,
        TestJson.toJson(
            Map.of(
                "include_fields",
                IncludeField.MIAIRR,
                "facets",
                RepertoireConstants.ID_FIELD)));

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
                        "field", RepertoireConstants.ID_FIELD, "value", false)))));
  }

  @Test
  public void repertoireSearchTicketAll() {
    var repertoireIdFields = Set.of(RepertoireConstants.UMA_ID_FIELD);

    var request =
        ModelFactory.buildAdcFilters(
            ModelFactory.buildComplexFilter(RepertoireConstants.ID_FIELD));
    var ticketRequest =
        TestCollections.mapMerge(
            request, ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD));

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RepertoireConstants.UMA_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    1),
                Pair.of(
                    TestCollections.getString(
                        secondRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    2)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId,
                TestConstants.UMA_ALL_SCOPES)); // repertoires have all the scopes

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchTicketSingle() {

    var request =
        ModelFactory.buildAdcFilters(
            ModelFactory.buildComplexFilter(RepertoireConstants.ID_FIELD));
    var ticketRequest =
        TestCollections.mapMerge(
            request, ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD));

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RepertoireConstants.UMA_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    1)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId,
                TestConstants.UMA_ALL_SCOPES)); // repertoires have all the scopes

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchTicketScopeLimit() {
    // based on fields limits to 'raw_sequence' scope
    var request = ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    var ticketRequest = ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD);

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RepertoireConstants.UMA_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    1)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchTicketFiltersEscalation() {
    Map<String, Object> filters =
        ModelFactory.buildAdcFilters(
            ModelFactory.buildSimpleFilter(
                "=", TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD, 12));
    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PUBLIC_FIELDS), filters);
    var ticketRequest =
        TestCollections.mapMerge(
            filters, ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD));

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RepertoireConstants.UMA_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    1)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchTicketIncludeFieldsEscalation() {
    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PUBLIC_FIELDS),
            ModelFactory.buildAdcIncludeFields("airr-core"));
    var ticketRequest = ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD);

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RepertoireConstants.UMA_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    1)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchIncludeFieldsTicketScopeLimit() {
    // based on fields limits to 'raw_sequence' scope
    var request = ModelFactory.buildAdcIncludeFields("airr-core");
    var ticketRequest = ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD);

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RepertoireConstants.UMA_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    1)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireFacetsTicket() {
    // based on facets limits to 'raw_sequence' scope
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    var ticketRequest = ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD);

    var repertoiresResponse =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RepertoireConstants.UMA_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                    1)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void rearrangementsFacetsTicket() {
    // based on facets limits to 'raw_sequence' scope
    var repertoireId =
        TestCollections.getString(
            this.firstRepertoire, RepertoireConstants.ID_FIELD);
    var request = ModelFactory.buildAdcFacets(TestConstants.REARRANGEMENT_PRIVATE_FIELD);

    var rearrangement = ModelFactory.buildRearrangement(repertoireId, "1");
    var ticketRequest = ModelFactory.buildAdcFacets(RearrangementConstants.REPERTOIRE_ID_FIELD);

    var response =
        ModelFactory.buildFacetsDocumentWithInfo(
            ModelFactory.buildFacets(
                RearrangementConstants.REPERTOIRE_ID_FIELD,
                Pair.of(
                    TestCollections.getString(
                        rearrangement, RearrangementConstants.REPERTOIRE_ID_FIELD),
                    1)));

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, response, ticketRequest);

    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            this.accessToken,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    this.requests.postJsonTicket(
        this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
        TestJson.toJson(request),
        ticket);
  }

  @Test
  public void repertoireSearchPublic() {
    Set<String> fields =
        Set.of(RepertoireConstants.ID_FIELD, RepertoireConstants.UMA_ID_FIELD);
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

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                TestCollections.mapSubset(this.firstRepertoire, fields),
                TestCollections.mapSubset(this.secondRepertoire, fields)));
  }

  @Test
  public void repertoireSearchAllAccess() {
    var request = Map.of();

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId, TestConstants.UMA_ALL_SCOPES));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                this.firstRepertoire, this.secondRepertoire));
  }

  @Test
  public void repertoireSearchAllAccessFullAdcQueryFieldFilterWithOversizedRpt() {
    var fields = Set.of(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    Map<String, Object> queryExtras =
        Map.of(
            "from", 1,
            "size", 1,
            "format", "json");

    Map<String, Object> queryFilters =
        ModelFactory.buildAdcFilters(
            ModelFactory.buildComplexFilter(RepertoireConstants.ID_FIELD));
    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFields(fields),
            ModelFactory.buildAdcIncludeFields("miairr"),
            queryFilters,
            queryExtras);

    var backendRequest =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFields(
                Set.of(
                    TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD,
                    RepertoireConstants.UMA_ID_FIELD)),
            ModelFactory.buildAdcIncludeFields("miairr"),
            queryFilters,
            queryExtras);

    var repertoiresResponse = ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                TestCollections.mapSubset(
                    this.firstRepertoire,
                    Sets.union(
                        fields,
                        Set.of(RepertoireConstants.STUDY_TITLE_FIELD))) // study title added by
                // include_fields
                ));
  }

  @Test
  public void repertoireSearchUnknownResponseFields() {
    var request = Map.of();

    var backendRepertoire = new HashMap<>(this.firstRepertoire);
    backendRepertoire.put(
        "field-xyz-unknown", 1); // would fail equality assert below if piped to requester
    var repertoiresResponse = ModelFactory.buildRepertoiresDocumentWithInfo(backendRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual)
        .isEqualTo(ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire));
  }

  @Test
  public void repertoireSearchPartialAccessDeny() {
    var request = Map.of();

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                TestCollections.mapSubset(this.firstRepertoire, RepertoireStatisticsScopeFields),
                TestCollections.mapSubset(this.secondRepertoire, RepertoirePublicFields)));
  }

  @Test
  public void repertoireSearchArrayPartialAccessDeny() {
    var request = Map.of();

    var repertoirePublic = TestCollections.mapSubset(this.firstRepertoire, RepertoirePublicFields);
    final List<String> files = List.of(TestConstants.generateHexString(10));
    var responseRepertoire1 =
        TestCollections.mapMerge(
            repertoirePublic,
            Map.of(
                "data_processing",
                List.of(
                    Map.of(
                        "data_processing_files", files, "numbo", TestConstants.Random.nextInt(20)),
                    Map.of( // should be removed
                        "numbo", TestConstants.Random.nextInt(20)))));

    var responseRepertoire2 =
        TestCollections.mapMerge(
            repertoirePublic,
            Map.of(
                "data_processing",
                List.of( // should filter this out
                    Map.of("numbo", TestConstants.Random.nextInt(20), "bool", false))));

    var expectedRepertoire1 =
        TestCollections.mapMerge(
            repertoirePublic,
            Map.of("data_processing", List.of(Map.of("data_processing_files", files))));

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(responseRepertoire1, responseRepertoire2);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(expectedRepertoire1, repertoirePublic));
  }

  @Test
  public void repertoireSearchDenyFiltersMatchPublicLeak() {
    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PUBLIC_FIELDS),
            ModelFactory.buildAdcFilters(
                ModelFactory.buildSimpleFilter(
                    "=", TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD, 12)));

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    // second repertoire is filtered out because of the "filters"
    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                TestCollections.mapSubset(
                    this.firstRepertoire, TestConstants.REPERTOIRE_PUBLIC_FIELDS)));
  }

  @Test
  public void repertoireSearchDenyFiltersMatchScopeLeak() {
    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PUBLIC_FIELDS),
            ModelFactory.buildAdcFilters(
                ModelFactory.buildSimpleFilter(
                    "=", TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD, 12)));

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    // second repertoire is filtered out because of the "filters"
    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                TestCollections.mapSubset(
                    this.firstRepertoire, TestConstants.REPERTOIRE_PUBLIC_FIELDS)));
  }

  @Test
  public void repertoireSearchPrivateFiltersGrant() {
    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PUBLIC_FIELDS),
            ModelFactory.buildAdcFilters(
                ModelFactory.buildSimpleFilter(
                    "=", TestConstants.REPERTOIRE_PRIVATE_STATISTICS_FIELD, "12")));

    var repertoiresResponse =
        ModelFactory.buildRepertoiresDocumentWithInfo(this.firstRepertoire, this.secondRepertoire);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId, Set.of(TestConstants.UMA_STATISTICS_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRepertoiresDocumentWithInfo(
                TestCollections.mapSubset(
                    this.firstRepertoire, TestConstants.REPERTOIRE_PUBLIC_FIELDS),
                TestCollections.mapSubset(
                    this.secondRepertoire, TestConstants.REPERTOIRE_PUBLIC_FIELDS)));
  }

  @Test
  public void rearrangementSearchAllAccess() {
    var repertoireId1 =
        TestCollections.getString(
            this.firstRepertoire, RepertoireConstants.ID_FIELD);
    var repertoireId2 =
        TestCollections.getString(
            this.secondRepertoire, RepertoireConstants.ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId1, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId2, "2");
    var rearrangementResponse =
        ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId, TestConstants.UMA_ALL_SCOPES));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200,
            token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2));
  }

  @Test
  public void rearrangementSearchTsv() throws IOException {
    var repertoireId1 =
        TestCollections.getString(
            this.firstRepertoire, RepertoireConstants.ID_FIELD);
    var repertoireId2 =
        TestCollections.getString(
            this.secondRepertoire, RepertoireConstants.ID_FIELD);
    var rearrangementFields =
        Set.of(
            RearrangementConstants.REPERTOIRE_ID_FIELD,
            RearrangementConstants.ID_FIELD,
            "sequence");

    var repositoryRequest = ModelFactory.buildAdcFields(rearrangementFields);
    var userRequest = TestCollections.mapMerge(repositoryRequest, ModelFactory.buildTsvFormat());

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId1, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId2, "2");
    var rearrangementResponse =
        ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock,
        TestConstants.REARRANGEMENT_PATH,
        200,
        rearrangementResponse,
        repositoryRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId, TestConstants.UMA_ALL_SCOPES));

    var actual =
        this.requests.postJsonPlain(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            userRequest,
            200,
            token);

    var actualRearrangements = parseTsv(actual);

    assertThat(actualRearrangements)
        .isEqualTo(
            List.of(
                TestCollections.mapSubset(rearrangement1, rearrangementFields),
                TestCollections.mapSubset(rearrangement2, rearrangementFields)));
  }

  @Test
  public void rearrangementSearchAllAccessSameRepertoire() {
    var repertoireId =
        TestCollections.getString(
            this.firstRepertoire, RepertoireConstants.ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId, "2");
    var rearrangementResponse =
        ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200,
            token);

    assertThat(actual)
        .isEqualTo(
            ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2));
  }

  @Test
  public void rearrangementSearchPartialAccessDeny() {
    var repertoireId1 =
        TestCollections.getString(
            this.firstRepertoire, RepertoireConstants.ID_FIELD);
    var repertoireId2 =
        TestCollections.getString(
            this.secondRepertoire, RepertoireConstants.ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId1, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId2, "2");
    var rearrangementResponse =
        ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId,
                Set.of(TestConstants.UMA_STATISTICS_SCOPE)) // should deny
            );

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200,
            token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1));
  }

  @Test
  public void rearrangementSearchPartialFullAccessDeny() {
    var repertoireId1 =
        TestCollections.getString(
            this.firstRepertoire, RepertoireConstants.ID_FIELD);
    var repertoireId2 =
        TestCollections.getString(
            this.secondRepertoire, RepertoireConstants.ID_FIELD);
    var request = Map.of();

    Map<String, Object> rearrangement1 = ModelFactory.buildRearrangement(repertoireId1, "1");
    Map<String, Object> rearrangement2 = ModelFactory.buildRearrangement(repertoireId2, "2");
    var rearrangementResponse =
        ModelFactory.buildRearrangementsDocumentWithInfo(rearrangement1, rearrangement2);

    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementResponse, request);

    var token = UmaWireMocker.wireTokenIntrospection(umaMock);

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200,
            token);

    assertThat(actual).isEqualTo(ModelFactory.buildRearrangementsDocumentWithInfo());
  }

  @Test
  public void repertoireFacetsAllAccess() {
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    var facet = ModelFactory.buildFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);

    var backendRequest =
        TestCollections.mapMerge(
            request,
            ModelFactory.buildAdcFilters(
                ModelFactory.buildAdcFacetsFilter(
                    RepertoireConstants.UMA_ID_FIELD,
                    List.of(
                        TestCollections.getString(
                            this.firstRepertoire, RepertoireConstants.UMA_ID_FIELD),
                        TestCollections.getString(
                            this.secondRepertoire, RepertoireConstants.UMA_ID_FIELD)))));

    var repertoiresResponse = ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES),
            ModelFactory.buildUmaResource(
                this.secondRepertoireUmaId, TestConstants.UMA_ALL_SCOPES));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void repertoireFacetsFiltersComposition() {
    Map<String, Object> requestFilter =
        ModelFactory.buildComplexFilter(RepertoireConstants.UMA_ID_FIELD);

    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD),
            ModelFactory.buildAdcFilters(requestFilter));
    var facet = ModelFactory.buildFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);

    var backendRequest =
        TestCollections.mapMerge(
            request,
            ModelFactory.buildAdcFilters(
                ModelFactory.buildAndFilter(
                    ModelFactory.buildAdcFacetsFilter(
                        RepertoireConstants.UMA_ID_FIELD,
                        List.of(
                            TestCollections.getString(
                                this.firstRepertoire, RepertoireConstants.UMA_ID_FIELD))),
                    requestFilter)));

    var repertoiresResponse = ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_ALL_SCOPES));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void repertoireFacetsPublicAccess() {
    var request = ModelFactory.buildAdcFacets(RepertoireConstants.UMA_ID_FIELD);
    var facet = ModelFactory.buildFacets(RepertoireConstants.UMA_ID_FIELD);

    var repertoiresResponse = ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, request);

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void repertoireFacetsDenyAccess() {
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    List<Map<String, Object>> facet = List.of();

    var backendRequest =
        TestCollections.mapMerge(
            request,
            ModelFactory.buildAdcFilters(
                ModelFactory.buildAdcFacetsFilter(
                    RepertoireConstants.UMA_ID_FIELD, List.of())));

    var repertoiresResponse = ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                TestConstants.generateHexString(10), Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void repertoireFacetsDenyAccessWhitelisting() {
    var request = ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);
    List<Map<String, Object>> facet =
        ModelFactory.buildFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD);

    var backendRequest =
        TestCollections.mapMerge(
            request,
            ModelFactory.buildAdcFilters(
                ModelFactory.buildAdcFacetsFilter(
                    RepertoireConstants.UMA_ID_FIELD,
                    List.of()) // for access denied, an empty 'in' is sent
                ));

    var repertoiresResponse = ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, backendRequest);

    var token = UmaWireMocker.wireTokenIntrospection(umaMock);

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 200, token);

    List<Map<String, Object>> expectedFacet = List.of();
    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(expectedFacet));
  }

  @Test
  public void rearrangementFacetsAllAccess() {
    var request = ModelFactory.buildAdcFacets(TestConstants.REARRANGEMENT_PRIVATE_FIELD);
    var facet = ModelFactory.buildFacets(TestConstants.REARRANGEMENT_PRIVATE_FIELD);

    var backendRequest =
        TestCollections.mapMerge(
            request,
            ModelFactory.buildAdcFilters(
                ModelFactory.buildAdcFacetsFilter(
                    RearrangementConstants.REPERTOIRE_ID_FIELD,
                    List.of(
                        TestCollections.getString(
                            this.firstRepertoire, RepertoireConstants.ID_FIELD)))));

    var rearrangementsResponse = ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementsResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                this.firstRepertoireUmaId, Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200,
            token);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void rearrangementFacetsDenyAccess() {
    var request = ModelFactory.buildAdcFacets(TestConstants.REARRANGEMENT_PRIVATE_FIELD);
    List<Map<String, Object>> facet = List.of();

    var backendRequest =
        TestCollections.mapMerge(
            request,
            ModelFactory.buildAdcFilters(
                ModelFactory.buildAdcFacetsFilter(
                    RearrangementConstants.REPERTOIRE_ID_FIELD, List.of())));

    var rearrangementsResponse = ModelFactory.buildFacetsDocumentWithInfo(facet);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REARRANGEMENT_PATH, 200, rearrangementsResponse, backendRequest);

    var token =
        UmaWireMocker.wireTokenIntrospection(
            umaMock,
            ModelFactory.buildUmaResource(
                TestConstants.generateHexString(10), Set.of(TestConstants.UMA_SEQUENCE_SCOPE)));

    var actual =
        this.requests.postJson(
            this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT),
            request,
            200,
            token);

    assertThat(actual).isEqualTo(ModelFactory.buildFacetsDocumentWithInfo(facet));
  }

  @Test
  public void postEndpointsErrorValidation() {

    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT),
        "invalid json \';>,{",
        400);

    var request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD),
            ModelFactory.buildAdcFields(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD));
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 422);

    request =
        TestCollections.mapMerge(
            ModelFactory.buildAdcFacets(TestConstants.REPERTOIRE_PRIVATE_SEQUENCE_FIELD),
            ModelFactory.buildAdcIncludeFields("miairr"));
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 422);

    request = ModelFactory.buildAdcIncludeFields("xyz029");
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 400);

    request = Map.of("fields", "invalid_schema");
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 400);

    String invalidField = "invalid_field_xyz2345";
    request = ModelFactory.buildAdcFacets(invalidField);
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 422);

    request = ModelFactory.buildAdcFields(invalidField);
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 422);

    request = ModelFactory.buildAdcFilters(ModelFactory.buildSimpleFilter("=", invalidField, "1"));
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 422);

    // invalid filter's value type
    request =
        ModelFactory.buildAdcFilters(
            ModelFactory.buildSimpleFilter("contains", "repertoire_id", 1));
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 400);

    // incompatible field type as specified in the CSV and the provided type
    request =
        ModelFactory.buildAdcFilters(
            ModelFactory.buildSimpleFilter("=", "data_processing.boolo", "1"));
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 422);

    request = ModelFactory.buildTsvFormat();
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT), request, 422);

    request =
        TestCollections.mapMerge(
            ModelFactory.buildTsvFormat(),
            ModelFactory.buildAdcFacets(TestConstants.REARRANGEMENT_PRIVATE_FIELD));
    this.requests.postJson(
        this.buildMiddlewareUrl(TestConstants.REARRANGEMENT_PATH_FRAGMENT), request, 422);
  }

  private static List<Map<String, Object>> parseTsv(String actual) throws IOException {
    var schema = CsvSchema.builder();
    schema.setColumnSeparator('\t');
    schema.setLineSeparator('\n');
    schema.setArrayElementSeparator(",");
    return (List<Map<String, Object>>)
        (List<?>)
            new CsvMapper()
                .readerFor(Map.class)
                .with(schema.build().withHeader())
                .readValues(actual)
                .readAll();
  }
}
