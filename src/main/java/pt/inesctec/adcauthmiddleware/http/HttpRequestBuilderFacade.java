package pt.inesctec.adcauthmiddleware.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class HttpRequestBuilderFacade {

  private HttpRequest.Builder builder;

  public HttpRequestBuilderFacade() {
    this.builder = HttpRequest.newBuilder();
  }

  public HttpRequestBuilderFacade getJson(URI uri) {
    this.builder =
        this.builder.uri(uri).GET().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  public HttpRequestBuilderFacade delete(URI uri) {
    this.builder = this.builder.uri(uri).DELETE();

    return this;
  }

  public HttpRequestBuilderFacade postForm(URI uri, Map<String, String> form) {
    var postBody = HttpRequestBuilderFacade.parseAsUrlEncodedForm(form);
    this.builder =
        this.builder
            .uri(uri)
            .POST(HttpRequest.BodyPublishers.ofString(postBody))
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

    return this;
  }

  public HttpRequestBuilderFacade postJson(URI uri, Object body) throws JsonProcessingException {
    var postBody = Json.toJson(body);

    this.builder =
        this.builder
            .uri(uri)
            .POST(HttpRequest.BodyPublishers.ofString(postBody))
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  public HttpRequestBuilderFacade putJson(URI uri, Object body) throws JsonProcessingException {
    var putBody = Json.toJson(body);

    this.builder =
        this.builder
            .uri(uri)
            .PUT(HttpRequest.BodyPublishers.ofString(putBody))
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  public HttpRequestBuilderFacade expectJson() {
    this.builder = this.builder.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  public HttpRequestBuilderFacade withBearer(String token) {
    this.builder = this.builder.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    return this;
  }

  public HttpRequestBuilderFacade withBasicAuth(String username, String password) {
    var basic = HttpHeaders.encodeBasicAuth(username, password, Charsets.UTF_8);
    this.builder = this.builder.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic);

    return this;
  }

  public HttpRequest.Builder getBuilder() {
    return builder;
  }

  public HttpRequest build() {
    return builder.build();
  }

  private static String parseAsUrlEncodedForm(Map<String, String> data) {

    return data.entrySet().stream()
        .map(
            e ->
                URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }
}
