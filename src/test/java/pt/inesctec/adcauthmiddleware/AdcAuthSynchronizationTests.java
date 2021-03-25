package pt.inesctec.adcauthmiddleware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.RepertoireConstants;
import pt.inesctec.adcauthmiddleware.utils.*;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class AdcAuthSynchronizationTests extends TestBase {
  private static WireMockServer umaMock = new WireMockRule(options().port(TestConstants.UMA_PORT));

  @BeforeAll
  public static void init() {
    backendMock.start();
    umaMock.start();
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
  }

  @Test
  public void testEmptySync() {
    wireSyncRepertoires();

    String accessToken = umaInit();
    UmaWireMocker.wireListResources(umaMock, accessToken);

    synchronize(accessToken);

    this.assertRepertoireNotFound("1");
  }

  @Test
  public void testCleanSlateSync() {
    var repertoire = ModelFactory.buildRepertoire("1");
    wireSyncRepertoires(repertoire);

    String accessToken = umaInit();
    UmaWireMocker.wireListResources(umaMock, accessToken);
    var umaId = UmaWireMocker.wireCreateResource(umaMock, repertoire, accessToken);

    synchronize(accessToken);

    assertRepertoireTicketRequest(repertoire, accessToken, umaId);
  }

  @Test
  public void testAdditiveSyncWithVerify() {
    var repertoire1 = ModelFactory.buildRepertoire("1");
    wireSyncRepertoires(repertoire1);
    String accessToken = umaInit();
    UmaWireMocker.wireListResources(umaMock, accessToken);
    var umaId1 = UmaWireMocker.wireCreateResource(umaMock, repertoire1, accessToken);
    synchronize(accessToken);
    assertRepertoireTicketRequest(repertoire1, accessToken, umaId1);

    umaMock.resetMappings();
    backendMock.resetMappings();

    var repertoire2 = ModelFactory.buildRepertoire("2");
    wireSyncRepertoires(repertoire1, repertoire2);

    var name = "name 123";
    accessToken = umaInit();
    UmaWireMocker.wireListResources(umaMock, accessToken, umaId1);
    UmaWireMocker.wireGetResource(umaMock, umaId1, name, List.of("123", TestConstants.UMA_SEQUENCE_SCOPE), accessToken);
    UmaWireMocker.wirePutResource(umaMock, umaId1, name, List.of("123", TestConstants.UMA_PUBLIC_SCOPE, TestConstants.UMA_SEQUENCE_SCOPE, TestConstants.UMA_STATISTICS_SCOPE), accessToken, AdcConstants.UMA_STUDY_TYPE);

    var umaId2 = UmaWireMocker.wireCreateResource(umaMock, repertoire2, accessToken);
    synchronize(accessToken);

    assertRepertoireTicketRequest(repertoire2, accessToken, umaId2);
    assertRepertoireTicketRequest(repertoire1, accessToken, umaId1);
    umaMock.verify(1, WireMock.putRequestedFor(WireMock.urlEqualTo(UmaWireMocker.UMA_RESOURCE_REGISTRATION_PATH + "/" + umaId1)));
  }

  @Test
  public void testDestructiveDbSync() {
    var repertoire1 = ModelFactory.buildRepertoire("1");
    wireSyncRepertoires(repertoire1);
    String accessToken = umaInit();
    UmaWireMocker.wireListResources(umaMock, accessToken);
    var umaId1 = UmaWireMocker.wireCreateResource(umaMock, repertoire1, accessToken);
    synchronize(accessToken);
    assertRepertoireTicketRequest(repertoire1, accessToken, umaId1);

    umaMock.resetMappings();
    backendMock.resetMappings();

    var repertoire2 = ModelFactory.buildRepertoire("2");
    wireSyncRepertoires(repertoire2);
    accessToken = umaInit();
    UmaWireMocker.wireListResources(umaMock, accessToken, umaId1);
    var umaId2 = UmaWireMocker.wireCreateResource(umaMock, repertoire2, accessToken);
    UmaWireMocker.wireDeleteResource(umaMock, umaId1, accessToken);
    var name = "name 123";
    UmaWireMocker.wireGetResource(umaMock, umaId1, name, List.of("123", TestConstants.UMA_SEQUENCE_SCOPE), accessToken);
    UmaWireMocker.wirePutResource(umaMock, umaId1, name, null, accessToken, AdcConstants.UMA_DELETED_STUDY_TYPE);
    synchronize(accessToken);
    assertRepertoireTicketRequest(repertoire2, accessToken, umaId2);
    assertRepertoireNotFound(TestCollections.getString(repertoire1, RepertoireConstants.ID_FIELD));
    umaMock.verify(1, WireMock.putRequestedFor(WireMock.urlEqualTo(UmaWireMocker.UMA_RESOURCE_REGISTRATION_PATH + "/" + umaId1)));
  }

  @Test
  public void testDestructiveUmaSync() {
    wireSyncRepertoires();
    String accessToken = umaInit();
    var danglingUmaId = "123";
    UmaWireMocker.wireListResources(umaMock, accessToken, danglingUmaId);
    var name = "name 123";
    UmaWireMocker.wireGetResource(umaMock, danglingUmaId, name, List.of("123", TestConstants.UMA_SEQUENCE_SCOPE), accessToken);
    UmaWireMocker.wirePutResource(umaMock, danglingUmaId, name, null, accessToken, AdcConstants.UMA_DELETED_STUDY_TYPE);
    synchronize(accessToken);

    umaMock.verify(1, WireMock.putRequestedFor(WireMock.urlEqualTo(UmaWireMocker.UMA_RESOURCE_REGISTRATION_PATH + "/" + danglingUmaId)));
  }

  private void assertRepertoireNotFound(String repertoireId) {
    this.requests.getJsonMap(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), 404);
  }


  private void assertRepertoireTicketRequest(Map<String, Object> repertoire, String accessToken, String umaId) {
    var repertoireId =
        TestCollections.getString(repertoire, RepertoireConstants.ID_FIELD);
    var ticket =
        UmaWireMocker.wireGetTicket(
            umaMock,
            accessToken,
            ModelFactory.buildUmaResource(
                umaId,
                TestConstants.UMA_ALL_SCOPES)); // repertoires have all the scopes

    this.requests.getJsonUmaTicket(
        this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), ticket);
  }

  private void synchronize(String accessToken) {
    //String accessToken = umaInit();
    this.requests.postEmpty(
        this.buildMiddlewareUrl(TestConstants.SYNCHRONIZE_PATH_FRAGMENT),
        accessToken,
        200);
  }

  private String umaInit() {
    UmaWireMocker.wireUmaWellKnown(umaMock);
    String token = UmaWireMocker.wireTokenEndpoint(umaMock);
    UmaWireMocker.wireSyncIntrospection(umaMock, token);
    return token;
  }


  private void wireSyncRepertoires(Object ... repertoires) {
    var searchRequest =
        ModelFactory.buildAdcFields(
            RepertoireConstants.ID_FIELD,
            RepertoireConstants.UMA_ID_FIELD,
            RepertoireConstants.STUDY_TITLE_FIELD);
    var repertoiresResponse = ModelFactory.buildRepertoiresDocumentWithInfo(repertoires);
    WireMocker.wirePostJson(
        backendMock, TestConstants.REPERTOIRE_PATH, 200, repertoiresResponse, searchRequest);
  }

}
