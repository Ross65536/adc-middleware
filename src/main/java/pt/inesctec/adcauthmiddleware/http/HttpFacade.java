package pt.inesctec.adcauthmiddleware.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HttpFacade {
  public static final HttpClient Client = HttpClient.newBuilder().build();
  static ObjectMapper JsonObjectMapper = new ObjectMapper();

  public static HttpRequest buildGetJsonRequest(URI url) {

    return HttpRequest.newBuilder()
        .uri(url)
        .GET()
        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  public static HttpRequest buildPostFormExpectJsonRequest(URI url, Map<String, String> form) {

    var postBody = HttpFacade.parseAsUrlEncodedForm(form);
    return HttpRequest.newBuilder()
        .uri(url)
        .POST(HttpRequest.BodyPublishers.ofString(postBody))
        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
        .headers("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .build();
  }

  public static HttpRequest.Builder buildJsonPostExpectJsonRequest(URI url, Object body) throws JsonProcessingException {
    var postBody = HttpFacade.toJson(body);

    return HttpRequest.newBuilder()
        .uri(url)
        .POST(HttpRequest.BodyPublishers.ofString(postBody))
        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
        .headers("Content-Type", MediaType.APPLICATION_JSON_VALUE);
  }

  public static HttpRequest.Builder addRequestBearer(HttpRequest.Builder request, String token) {
    return request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private static String toJson(Object body) throws JsonProcessingException {
    return JsonObjectMapper.writeValueAsString(body);
  }

  public static <T> T makeExpectJsonRequest(HttpRequest request, Class<T> respClass) throws IOException, InterruptedException {
    HttpResponse<Supplier<T>> response = HttpFacade.Client.send(request, new JsonBodyHandler<>(respClass));

    HttpFacade.validateOkResponse(response);
    HttpFacade.validateJsonResponseHeader(response);

    // TODO figure out how to check malformed JSON errors
    return response.body().get();
  }

  public static String makeExpectJsonStringRequest(HttpRequest request) throws IOException, InterruptedException {
    var response =  HttpFacade.Client.send(request, HttpResponse.BodyHandlers.ofString());

    HttpFacade.validateOkResponse(response);
    HttpFacade.validateJsonResponseHeader(response);

    return response.body();
  }

  private static String parseAsUrlEncodedForm(Map<String, String> data) {

    return data.entrySet()
        .stream()
        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }

  private static void validateOkResponse(HttpResponse response) throws IOException {
    int statusCode = response.statusCode();
    int respFamily = statusCode / 100;
    if (respFamily != 2) {
      throw new IOException("Unexpected response code: " + statusCode);
    }
  }

  private static void validateJsonResponseHeader(HttpResponse response) throws IOException {
    var values = response.headers().allValues("Content-Type");
    if (values.size() == 0) {
      throw new IOException("Response contains no content-type header");
    }

    var anyJson = values.stream()
          .anyMatch(val -> val.contains(MediaType.APPLICATION_JSON_VALUE));

    if (!anyJson) {
      throw new IOException("Response isn't JSON");
    }
  }
}
