package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.utils.AdcWireMocker;
import pt.inesctec.adcauthmiddleware.utils.ModelFactory;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;
import pt.inesctec.adcauthmiddleware.utils.UmaWireMocker;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdcAuthEndpointTests extends TestBase {

  private static WireMockServer umaMock = new WireMockRule(options().port(TestConstants.UMA_PORT));


  @BeforeAll
  public void sync() throws JsonProcessingException {

    var searchRequest = ModelFactory.buildAdcSearch(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_ID_FIELD, AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD);

    Map<String, Object> firstRepertoire = ModelFactory.buildRepertoire("1");
    Map<String, Object> secondRepertoire = ModelFactory.buildRepertoire("2");
    var repertoiresResponse = ModelFactory.buildRepertoiresDocument(
        ModelFactory.buildInfo(),
        firstRepertoire,
        secondRepertoire
    );

    AdcWireMocker.wireRepertoiresSearch(backendMock, repertoiresResponse, searchRequest);
    backendMock.start();

    UmaWireMocker.wireUmaWellKnown(umaMock);
    var accessToken = UmaWireMocker.wireTokenEndpoint(umaMock);
    UmaWireMocker.wireListResources(umaMock, accessToken);
    UmaWireMocker.wireCreateResource(umaMock, firstRepertoire, accessToken);
    UmaWireMocker.wireCreateResource(umaMock, secondRepertoire, accessToken);
    umaMock.start();

    this.requests.postEmpty(this.buildMiddlewareUrl("synchronize"), TestConstants.SYNC_PASSWORD, 200);
  }

  @Test
  public void synchronizeOk() {
    // empty because the synchronization is done in sync()
    // if tests fail check logs that resources are created, since they can fail to create without failing the synchronize
  }

}
