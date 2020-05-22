package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.utils.ModelFactory;
import pt.inesctec.adcauthmiddleware.utils.TestCollections;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;
import pt.inesctec.adcauthmiddleware.utils.UmaWireMocker;
import pt.inesctec.adcauthmiddleware.utils.WireMocker;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

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

    var searchRequest = ModelFactory.buildAdcSearch(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD);

    this.firstRepertoire = ModelFactory.buildRepertoire("1");
    this.secondRepertoire = ModelFactory.buildRepertoire("2");
    var repertoiresResponse = ModelFactory.buildRepertoiresDocumentWithInfo(
        firstRepertoire,
        secondRepertoire
    );

    WireMocker.wirePostJson(backendMock, TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT), 200, repertoiresResponse, searchRequest);
    backendMock.start();

    UmaWireMocker.wireUmaWellKnown(umaMock);
    this.accessToken = UmaWireMocker.wireTokenEndpoint(umaMock);
    UmaWireMocker.wireListResources(umaMock, accessToken);
    this.firstRepertoireUmaId = UmaWireMocker.wireCreateResource(umaMock, firstRepertoire, accessToken);
    this.secondRepertoireUmaId = UmaWireMocker.wireCreateResource(umaMock, secondRepertoire, accessToken);
    umaMock.start();

    this.requests.postEmpty(this.buildMiddlewareUrl(TestConstants.SYNCHRONIZE_PATH_FRAGMENT), TestConstants.SYNC_PASSWORD, 200);
  }

  @BeforeEach
  public void reset() throws JsonProcessingException {
    backendMock.resetMappings();
    umaMock.resetMappings();
    this.accessToken = UmaWireMocker.wireTokenEndpoint(umaMock);
  }

  @Test
  public void synchronizeOk() {
    // empty because the synchronization is done in init()
    // if tests fail check logs that resources are created, since they can fail to create without failing the synchronize
  }

  @Test
  public void singleRepertoireTicketOk() throws JsonProcessingException {
    var repertoireId = TestCollections.getString(firstRepertoire, AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD);

    WireMocker.wireGetJson(backendMock, TestConstants.buildAirrPath(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), 200, ModelFactory.buildRepertoiresDocumentWithInfo(firstRepertoire));

    var ticket = UmaWireMocker.wireGetTicket(umaMock, this.accessToken, ModelFactory.buildUmaResource(this.firstRepertoireUmaId, TestConstants.UMA_SCOPES));

    this.requests.getJsonUmaTicket(this.buildMiddlewareUrl(TestConstants.REPERTOIRE_PATH_FRAGMENT, repertoireId), ticket);
  }

}
