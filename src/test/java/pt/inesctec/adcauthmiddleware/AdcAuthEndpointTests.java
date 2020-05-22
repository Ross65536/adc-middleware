package pt.inesctec.adcauthmiddleware;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdcAuthEndpointTests extends TestBase {

  @BeforeAll
  public void sync() {

    this.requests.postEmpty(this.buildMiddlewareUrl("synchronize"), "master", 200);
  }

  @Test
  public void a() {

  }

}
