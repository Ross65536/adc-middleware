package pt.inesctec.adcauthmiddleware.http;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class HttpFacade {
  public static final HttpClient Client = HttpClient.newBuilder().build();

  public static void makeRequest(HttpRequest request) throws IOException, InterruptedException {
    var response = HttpFacade.Client.send(request, HttpResponse.BodyHandlers.discarding());
    HttpFacade.validateOkResponseDiscarding(response);
  }

  public static <T> T makeExpectJsonRequest(HttpRequest request, Class<T> respClass)
      throws IOException, InterruptedException {
    var inputStream = HttpFacade.makeExpectJsonAsStreamRequest(request);
    return Json.parseJson(respClass, inputStream);
  }

  public static InputStream makeExpectJsonAsStreamRequest(HttpRequest request)
      throws IOException, InterruptedException {
    var response = HttpFacade.Client.send(request, HttpResponse.BodyHandlers.ofInputStream());

    HttpFacade.validateOkResponse(response);
    HttpFacade.validateJsonResponseHeader(response);

    return response.body();
  }

  private static void validateOkResponseDiscarding(HttpResponse response) throws IOException {
    int statusCode = response.statusCode();
    int respFamily = statusCode / 100;
    if (respFamily != 2) {
      throw new IOException("Unexpected response code: " + statusCode);
    }
  }

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
