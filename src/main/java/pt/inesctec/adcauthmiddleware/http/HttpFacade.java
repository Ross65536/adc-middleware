package pt.inesctec.adcauthmiddleware.http;

import org.springframework.http.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

public class HttpFacade {



  public static final HttpClient Client = HttpClient.newBuilder().build();

  public static <T> T getJson(URI url, Class<T> respClass) throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder()
        .uri(url)
        .GET()
        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build();

    HttpResponse<Supplier<T>> response = HttpFacade.Client.send(request, new JsonBodyHandler<>(respClass));

    HttpFacade.validateOkResponse(response);
    HttpFacade.validateJsonResponseHeader(response);

    // TODO figure out how to check malformed JSON
    return response.body().get();
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
