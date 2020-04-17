package pt.inesctec.adcauthmiddleware;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;


@RestController
public class AdcController {
  private static HttpClient client = HttpClient.newBuilder().build();

  @Autowired private AdcConfiguration adcConfig;

  @Autowired private UmaConfig umaConfig;

  @RequestMapping(
      value = "/study/{studyId}/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public String greeting(@PathVariable String studyId, @PathVariable String repertoireId)
      throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath("study", studyId, "repertoire", repertoireId);

    HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    return response.body();
  }

  private URI getResourceServerPath(String... parts) {
    final String basePath = adcConfig.getResourceServerUrl();
    return Utils.buildUrl(basePath, parts);
  }
}
