package pt.inesctec.adcauthmiddleware.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class Requests {
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    @Autowired private TestRestTemplate restTemplate;

    public Map<String, Object> getJsonMap(String path, int expectedStatus) {
        var entity = this.restTemplate.getForEntity(path, String.class);
        assertThat(entity.getStatusCodeValue()).isEqualTo(expectedStatus);
        return TestJson.fromJson(entity.getBody(), Map.class);
    }

    public Map<String, Object> getJsonMap(String path, int expectedStatus, String token) {
        HttpEntity<String> entity = new HttpEntity<>(buildAuthorizationHeader(token));

        var respEntity = restTemplate.exchange(path, HttpMethod.GET, entity, String.class);
        assertThat(respEntity.getStatusCodeValue()).isEqualTo(expectedStatus);
        return TestJson.fromJson(respEntity.getBody(), Map.class);
    }

    private static final Pattern TicketPattern = Pattern.compile("^.*ticket=\"(.*)\"$");

    public void getJsonUmaTicket(String path, String expectedTicket) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Protected", "true");

        HttpEntity httpEntity = new HttpEntity(headers);

        var entity = this.restTemplate.exchange(
            path, HttpMethod.GET, httpEntity, String.class);

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
        ResponseEntity<String> restEntity = postJsonPlain(url, json, 401, headers);

        checkTicket(expectedTicket, restEntity);
    }

    public void postEmpty(String url, String bearer, int expectedStatus) {
        HttpEntity<String> entity = new HttpEntity<>(buildAuthorizationHeader(bearer));

        var restEntity = this.restTemplate.postForEntity(url, entity, String.class);
        assertThat(restEntity.getStatusCodeValue()).isEqualTo(expectedStatus);
    }

    public Map<String, Object> postJson(String url, String json, int expectedStatus) {
        HttpHeaders headers = new HttpHeaders();
        return postJson(url, json, expectedStatus, headers);
    }

    public Map<String, Object> postJson(String url, Object json, int expectedStatus) {
        return postJson(url, TestJson.toJson(json), expectedStatus);
    }

    public Map<String, Object> postJson(String url, Object request, int expectedStatus, String token) {
        var headers = buildAuthorizationHeader(token);
        return postJson(url, TestJson.toJson(request), expectedStatus, headers);
    }

    public String postJsonPlain(String url, Object request, int expectedStatus, String token) {
        var headers = buildAuthorizationHeader(token);
        return postJsonPlain(url, TestJson.toJson(request), expectedStatus, headers).getBody();
    }

    private Map<String, Object> postJson(String url, String json, int expectedStatus, HttpHeaders headers) {
        ResponseEntity<String> restEntity = postJsonPlain(url, json, expectedStatus, headers);
        return TestJson.fromJson(restEntity.getBody(), Map.class);
    }

    private ResponseEntity<String> postJsonPlain(String url, String json, int expectedStatus, HttpHeaders headers) {
        headers.set(WireMocker.CONTENT_TYPE_HEADER, WireMocker.JSON_MIME);
        headers.set(WireMocker.ACCEPT_HEADER, WireMocker.JSON_MIME);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        var restEntity = this.restTemplate.postForEntity(url, entity, String.class);
        assertThat(restEntity.getStatusCodeValue()).isEqualTo(expectedStatus);
        return restEntity;
    }

    private HttpHeaders buildAuthorizationHeader(String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + bearer);
        return headers;
    }
}
