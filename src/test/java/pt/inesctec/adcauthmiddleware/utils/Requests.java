package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

  public void postEmpty(String url, String bearer, int expectedStatus) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + bearer);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    var restEntity = this.restTemplate.postForEntity(url, entity, String.class);
    assertThat(restEntity.getStatusCodeValue()).isEqualTo(expectedStatus);
  }

}
