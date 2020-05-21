package pt.inesctec.adcauthmiddleware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.ClassRule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import pt.inesctec.adcauthmiddleware.utils.TestConstants;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestBase {
  @ClassRule
  protected static WireMockServer backendMock = new WireMockRule(options().port(TestConstants.BACKEND_PORT));

  @LocalServerPort
  protected int port;

  protected String buildMiddlewarePath(String path) {
    return TestConstants.BASE_MIDDLEWARE_PATH + "/" + path;
  }

  protected String buildMiddlewareUrl(String path) {
    return "http://localhost:" + port + this.buildMiddlewarePath(path);
  }

}
