package pt.inesctec.adcauthmiddleware.adc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.utils.Utils;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementIds;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireIds;
import pt.inesctec.adcauthmiddleware.adc.models.internal.AdcIdsResponse;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

@Component
public class AdcClient {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcClient.class);

  private final AdcConfiguration adcConfig;

  public AdcClient(AdcConfiguration adcConfig) {
    this.adcConfig = adcConfig;
  }

  public InputStream getResource(String path) throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath(path);

    var request = new HttpRequestBuilderFacade()
        .getJson(uri)
        .build();

    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public InputStream getRepertoireAsStream(String repertoireId) throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath("repertoire", repertoireId);

    var request = new HttpRequestBuilderFacade()
        .getJson(uri)
        .build();

    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public InputStream getRearrangementAsStream(String rearrangementId) throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath("rearrangement", rearrangementId);

    var request = new HttpRequestBuilderFacade()
        .getJson(uri)
        .build();

    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public InputStream searchRepertoiresAsStream(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.getFacets() == null);
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var request = this.buildSearchRequest("repertoire", adcRequest);
    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public InputStream searchRearrangementsAsStream(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.getFacets() == null);
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var request = this.buildSearchRequest("rearrangement", adcRequest);
    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public List<RepertoireIds> getRepertoireIds(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.getFacets() == null);
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var request = this.buildSearchRequest("repertoire", adcRequest);
    var repertoires = HttpFacade.makeExpectJsonRequest(request, AdcIdsResponse.class)
          .getRepertoires();

    return listPostConditions(repertoires);
  }


  public List<RearrangementIds> getRearrangementIds(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.getFacets() == null);
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var request = this.buildSearchRequest("rearrangement", adcRequest);
    var rearrangements = HttpFacade.makeExpectJsonRequest(request, AdcIdsResponse.class)
        .getRearrangements();

    return listPostConditions(rearrangements);
  }

  private HttpRequest buildSearchRequest(String path, AdcSearchRequest adcSearchRequest) throws JsonProcessingException {
    final URI uri = this.getResourceServerPath(path);
    return new HttpRequestBuilderFacade()
        .postJson(uri, adcSearchRequest)
        .build();
  }

  private URI getResourceServerPath(String... parts) {
    final String basePath = adcConfig.getResourceServerUrl();
    return Utils.buildUrl(basePath, parts);
  }

  private static <T> List<T> listPostConditions(List<T> resources) throws Exception {
    Utils.assertNotNull(resources);
    Utils.jaxValidateList(resources);

    return resources;
  }
}
