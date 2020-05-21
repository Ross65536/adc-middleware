package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;
import static org.assertj.core.api.Assertions.assertThat;

@Component
public class Requests {
  @Autowired
  private TestRestTemplate restTemplate;

  public Map<String, Object> getJsonMap(String path, int expectedStatus)
      throws JsonProcessingException {
    var entity = this.restTemplate.getForEntity(path, String.class);
    assertThat(entity.getStatusCodeValue()).isEqualTo(expectedStatus);
    return TestJson.fromJson(entity.getBody(), Map.class);
  }

}
