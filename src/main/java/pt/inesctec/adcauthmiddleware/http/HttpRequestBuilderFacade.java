package pt.inesctec.adcauthmiddleware.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps around {@link HttpRequest.Builder} providing utility methods. Follows Builder pattern with fluent interface.
 */
public class HttpRequestBuilderFacade {

  private HttpRequest.Builder builder;

  public HttpRequestBuilderFacade() {
    this.builder = HttpRequest.newBuilder();
  }

  /**
   * For making a GET request and request JSON response.
   *
   * @param uri request URL.
   * @return this instance.
   */
  public HttpRequestBuilderFacade getJson(URI uri) {
    this.builder =
        this.builder.uri(uri).GET().setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  /**
   * For making a DELETE request.
   *
   * @param uri request URL.
   * @return this instance.
   */
  public HttpRequestBuilderFacade delete(URI uri) {
    this.builder = this.builder.uri(uri).DELETE();

    return this;
  }

  /**
   * For making a POST request with a 'x-www-form-urlencoded' content type.
   *
   * @param uri the request URL.
   * @param form the form body contents in map form.
   * @return this instance.
   */
  public HttpRequestBuilderFacade postForm(URI uri, Map<String, String> form) {
    var postBody = HttpRequestBuilderFacade.parseAsUrlEncodedForm(form);
    this.builder =
        this.builder
            .uri(uri)
            .POST(HttpRequest.BodyPublishers.ofString(postBody))
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

    return this;
  }

  /**
   * For making a POST request with a JSON content type.
   *
   * @param uri the request URL.
   * @param body the source for the JSON object.
   * @return this instance
   * @throws JsonProcessingException on JSON encode error.
   */
  public HttpRequestBuilderFacade postJson(URI uri, Object body) throws JsonProcessingException {
    var postBody = Json.toJson(body);

    this.builder =
        this.builder
            .uri(uri)
            .POST(HttpRequest.BodyPublishers.ofString(postBody))
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  /**
   * For making a PUT request with a JSON content-type body.
   *
   * @param uri the request URL
   * @param body the JSON source object.
   * @return this isntance
   * @throws JsonProcessingException on JSON encode error
   */
  public HttpRequestBuilderFacade putJson(URI uri, Object body) throws JsonProcessingException {
    var putBody = Json.toJson(body);

    this.builder =
        this.builder
            .uri(uri)
            .PUT(HttpRequest.BodyPublishers.ofString(putBody))
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  /**
   * Add a header for expecting a JSON response.
   *
   * @return this instace
   */
  public HttpRequestBuilderFacade expectJson() {
    this.builder = this.builder.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    return this;
  }

  /**
   * Add an 'Authorization' 'Bearer' header.
   *
   * @param token the bearer token value (without the 'Bearer ').
   * @return this instance.
   */
  public HttpRequestBuilderFacade withBearer(String token) {
    this.builder = this.builder.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    return this;
  }

  /**
   * Add a 'Authorization' 'Basic' header.
   *
   * @param username the username.
   * @param password the password.
   * @return this instance.
   */
  public HttpRequestBuilderFacade withBasicAuth(String username, String password) {
    var basic = HttpHeaders.encodeBasicAuth(username, password, Charsets.UTF_8);
    this.builder = this.builder.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic);

    return this;
  }

  /**
   * Build the request.
   *
   * @return the request
   */
  public HttpRequest build() {
    return builder.build();
  }

  /**
   * Process map to obtain url encoded string body.
   *
   * @param data the form body.
   * @return encoded string.
   */
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
