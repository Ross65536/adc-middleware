package pt.inesctec.adcauthmiddleware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdcAuthMiddlewareApplicationTests {

  @LocalServerPort
  private int port;
  @Autowired
  private TestRestTemplate restTemplate;

  @ClassRule
  private static WireMockServer backend = new WireMockRule(options().port(8883));

  @BeforeClass
  public void init() {
  }

  @Test
  void publicInfo() {
    int status = 200;

    backend.stubFor(WireMock.get("/airr/v1/info")
        .willReturn(WireMock.aResponse()
          .withStatus(status)
          .withBody("{}")
            .withHeader("Content-Type", "application/json")
        )
    );
    backend.start();


    var entity = this.restTemplate.getForEntity("http://localhost:" + port + "/airr/v1/info", String.class);
    assertThat(entity.getStatusCodeValue()).isEqualTo(status);


  }
}
