package pt.inesctec.adcauthmiddleware.adc;

import pt.inesctec.adcauthmiddleware.Utils;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

public class AdcClient {
  private final AdcConfiguration adcConfig;

  public AdcClient(AdcConfiguration adcConfig) {
    this.adcConfig = adcConfig;
  }

  public String getRepertoireAsString(String repertoireId) throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath("repertoire", repertoireId);

    var request = new HttpRequestBuilderFacade()
        .getJson(uri)
        .build();

    return HttpFacade.makeExpectJsonStringRequest(request);
  }

  private URI getResourceServerPath(String... parts) {
    final String basePath = adcConfig.getResourceServerUrl();
    return Utils.buildUrl(basePath, parts);
  }
}
