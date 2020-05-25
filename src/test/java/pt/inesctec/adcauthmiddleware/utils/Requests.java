package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import static org.assertj.core.api.Assertions.assertThat;

@Component
public class Requests {
  private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

  @Autowired private TestRestTemplate restTemplate;

  public Map<String, Object> getJsonMap(String path, int expectedStatus)
      throws JsonProcessingException {
    var entity = this.restTemplate.getForEntity(path, String.class);
    assertThat(entity.getStatusCodeValue()).isEqualTo(expectedStatus);
    return TestJson.fromJson(entity.getBody(), Map.class);
  }

  public Map<String, Object> getJsonMap(String path, int expectedStatus, String token)
      throws JsonProcessingException {
    HttpEntity<String> entity = new HttpEntity<>(buildAuthorizationHeader(token));

    var respEntity = restTemplate.exchange(path, HttpMethod.GET, entity, String.class);
    assertThat(respEntity.getStatusCodeValue()).isEqualTo(expectedStatus);
    return TestJson.fromJson(respEntity.getBody(), Map.class);
  }

  private static final Pattern TicketPattern = Pattern.compile("^.*ticket=\"(.*)\"$");

  public void getJsonUmaTicket(String path, String expectedTicket) {
    var entity = this.restTemplate.getForEntity(path, String.class);
    assertThat(entity.getStatusCodeValue()).isEqualTo(401);

    checkTicket(expectedTicket, entity);
  }

  private void checkTicket(String expectedTicket, ResponseEntity<String> entity) {
    var authorizations = entity.getHeaders().get(WWW_AUTHENTICATE_HEADER);
    assertThat(authorizations).isNotNull().hasSize(1);
    var authorization = authorizations.get(0);

    Matcher matches = TicketPattern.matcher(authorization);
    assertThat(matches.matches()).isTrue();

    String actualTicket = matches.group(1);
    assertThat(actualTicket).isEqualTo(expectedTicket);
  }

  public void postJsonTicket(String url, String json, String expectedTicket) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(WireMocker.CONTENT_TYPE_HEADER, WireMocker.JSON_MIME);
    headers.set(WireMocker.ACCEPT_HEADER, WireMocker.JSON_MIME);
    HttpEntity<String> entity = new HttpEntity<>(json, headers);

    var restEntity = this.restTemplate.postForEntity(url, entity, String.class);
    assertThat(restEntity.getStatusCodeValue()).isEqualTo(401);

    checkTicket(expectedTicket, restEntity);
  }

  public void postEmpty(String url, String bearer, int expectedStatus) {
    HttpEntity<String> entity = new HttpEntity<>(buildAuthorizationHeader(bearer));

    var restEntity = this.restTemplate.postForEntity(url, entity, String.class);
    assertThat(restEntity.getStatusCodeValue()).isEqualTo(expectedStatus);
  }

  public Map<String, Object> postJson(String url, String json, int expectedStatus) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(WireMocker.CONTENT_TYPE_HEADER, WireMocker.JSON_MIME);
    headers.set(WireMocker.ACCEPT_HEADER, WireMocker.JSON_MIME);
    HttpEntity<String> entity = new HttpEntity<>(json, headers);

    var restEntity = this.restTemplate.postForEntity(url, entity, String.class);
    assertThat(restEntity.getStatusCodeValue()).isEqualTo(expectedStatus);
    return TestJson.fromJson(restEntity.getBody(), Map.class);
  }

  private HttpHeaders buildAuthorizationHeader(String bearer) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + bearer);
    return headers;
  }
}
