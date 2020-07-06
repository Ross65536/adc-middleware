package pt.inesctec.adcauthmiddleware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import pt.inesctec.adcauthmiddleware.utils.Requests;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestBase {

  protected static WireMockServer backendMock = new WireMockRule(options().port(TestConstants.BACKEND_PORT));

  @Autowired
  protected Requests requests;


  @LocalServerPort
  protected int port;

  protected String buildMiddlewareUrl(String ... path) {
    return "http://localhost:" + port + TestConstants.buildAirrPath(path);
  }

}
