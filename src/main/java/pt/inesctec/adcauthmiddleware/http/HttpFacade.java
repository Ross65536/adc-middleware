package pt.inesctec.adcauthmiddleware.http;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Utility methods for making HTTP requests using java 11 HTTP library.
 */
public class HttpFacade {
  private static final HttpClient Client = HttpClient.newBuilder().build();

  /**
   * Make the HTTP request while discarding the response. Will validate that the response if OK.
   *
   * @param request the HTTP request
   * @throws IOException on error
   * @throws InterruptedException on error
   */
  public static void makeRequest(HttpRequest request) throws IOException, InterruptedException {
    var response = HttpFacade.Client.send(request, HttpResponse.BodyHandlers.discarding());
    HttpFacade.validateOkResponseDiscarding(response);
  }

  /**
   * Make the HTTP request and expect JSON response. Parse the response into model.
   * Will validate that the response if OK.
   *
   * @param request the HTTP request
   * @param respClass the target model
   * @param <T> the target model type
   * @return the model
   * @throws IOException on error
   * @throws InterruptedException on error
   */
  public static <T> T makeExpectJsonRequest(HttpRequest request, Class<T> respClass)
      throws IOException, InterruptedException {
    var inputStream = HttpFacade.makeExpectJsonAsStreamRequest(request);
    return Json.parseJson(respClass, inputStream);
  }

  /**
   * Make the HTTP request and expect JSON stream.
   * Like {@link #makeExpectJsonRequest(HttpRequest, Class)} but the response isn't parsed and instead the byte stream is returned.
   * Will validate that the response if OK.
   *
   * @param request the HTTP request.
   * @return the byte stream
   * @throws IOException on error
   * @throws InterruptedException on error
   */
  public static InputStream makeExpectJsonAsStreamRequest(HttpRequest request)
      throws IOException, InterruptedException {
    var response = HttpFacade.Client.send(request, HttpResponse.BodyHandlers.ofInputStream());

    HttpFacade.validateOkResponse(response);
    HttpFacade.validateJsonResponseHeader(response);

    return response.body();
  }

  /**
   * Validate that the response OK and discard the response body on error (status family != 2).
   *
   * @param response the HTTP response.
   * @throws IOException when not OK response.
   */
  private static void validateOkResponseDiscarding(HttpResponse response) throws IOException {
    int statusCode = response.statusCode();
    int respFamily = statusCode / 100;
    if (respFamily != 2) {
      throw new IOException("Unexpected response code: " + statusCode);
    }
  }

  /**
   * Like {@link #validateOkResponse(HttpResponse)} but the body and content type header
   * is saved in the exception on HTTP error.
   *
   * @param response the HTTP response.
   * @throws IOException when not OK response.
   */
  private static void validateOkResponse(HttpResponse<InputStream> response) throws IOException {
    int statusCode = response.statusCode();
    int respFamily = statusCode / 100;
    if (respFamily != 2) {
      var is = response.body();
      var contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE);
      var stringBody = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
      throw new ClientError(statusCode, stringBody, contentType);
    }
  }

  /**
   * Checks that the HTTP response has the header content-type set to JSON.
   *
   * @param response the HTTP response
   * @throws IOException when header is missing.
   */
  private static void validateJsonResponseHeader(HttpResponse response) throws IOException {
    var values = response.headers().allValues(HttpHeaders.CONTENT_TYPE);
    if (values.size() == 0) {
      throw new IOException("Response contains no content-type header");
    }

    var anyJson = values.stream().anyMatch(val -> val.contains(MediaType.APPLICATION_JSON_VALUE));

    if (!anyJson) {
      throw new IOException("Response isn't JSON");
    }
  }
}
