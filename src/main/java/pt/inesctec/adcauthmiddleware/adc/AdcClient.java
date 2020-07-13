package pt.inesctec.adcauthmiddleware.adc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementIds;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireIds;
import pt.inesctec.adcauthmiddleware.adc.models.internal.AdcFacetsResponse;
import pt.inesctec.adcauthmiddleware.adc.models.internal.AdcIdsResponse;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

@Component
public class AdcClient {

  private final AdcConfiguration adcConfig;

  public AdcClient(AdcConfiguration adcConfig) {
    this.adcConfig = adcConfig;
  }

  public InputStream getResource(String path) throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath(path);

    var request = new HttpRequestBuilderFacade().getJson(uri).build();

    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public InputStream getRepertoireAsStream(String repertoireId)
      throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath("repertoire", repertoireId);

    var request = new HttpRequestBuilderFacade().getJson(uri).build();

    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public InputStream getRearrangementAsStream(String rearrangementId)
      throws IOException, InterruptedException {
    final URI uri = this.getResourceServerPath("rearrangement", rearrangementId);

    var request = new HttpRequestBuilderFacade().getJson(uri).build();

    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public List<RearrangementIds> getRearrangement(String rearrangementId) throws Exception {
    final URI uri = this.getResourceServerPath("rearrangement", rearrangementId);
    var request = new HttpRequestBuilderFacade().getJson(uri).build();
    var rearrangements =
        HttpFacade.makeExpectJsonRequest(request, AdcIdsResponse.class).getRearrangements();

    listPostConditions(rearrangements);
    return rearrangements;
  }

  public InputStream searchRepertoiresAsStream(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var request = this.buildSearchRequest("repertoire", adcRequest);
    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public InputStream searchRearrangementsAsStream(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var request = this.buildSearchRequest("rearrangement", adcRequest);
    return HttpFacade.makeExpectJsonAsStreamRequest(request);
  }

  public List<RepertoireIds> getRepertoireIds(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.getFacets() == null);
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var request = this.buildSearchRequest("repertoire", adcRequest);
    var repertoires =
        HttpFacade.makeExpectJsonRequest(request, AdcIdsResponse.class).getRepertoires();

    return listPostConditions(repertoires);
  }

  public Set<String> getRepertoireStudyIds(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var idsQuery = adcRequest.queryClone().withFacets(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);
    var request = this.buildSearchRequest("repertoire", idsQuery);
    var facets =
            HttpFacade.makeExpectJsonRequest(request, AdcFacetsResponse.class).getFacets();

    // TODO update field once ireceptor-turnkey fixes their facets bug
    return processStringFacets(facets, AdcConstants.REPERTOIRE_STUDY_ID_BASE);
  }

  public Set<String> getRearrangementRepertoireIds(AdcSearchRequest adcRequest) throws Exception {
    Preconditions.checkArgument(adcRequest.isJsonFormat());

    var idsQuery = adcRequest.queryClone().withFacets(AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);
    var request = this.buildSearchRequest("rearrangement", idsQuery);
    var facets =
        HttpFacade.makeExpectJsonRequest(request, AdcFacetsResponse.class).getFacets();

    return processStringFacets(facets, AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);
  }

  private HttpRequest buildSearchRequest(String path, AdcSearchRequest adcSearchRequest)
      throws JsonProcessingException {
    final URI uri = this.getResourceServerPath(path);
    return new HttpRequestBuilderFacade().postJson(uri, adcSearchRequest).expectJson().build();
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

  private static Set<String> processStringFacets(List<Map<String, Object>> facets, String facetsField) throws Exception {
    Utils.assertNotNull(facets);
    CollectionsUtils.assertMapListContainsKeys(facets, facetsField);

    return facets.stream()
        .filter(
            facet -> {
              var count = (Integer) facet.get("count");
              return count > 0;
            })
        .map(facet -> (String) facet.get(facetsField))
        .collect(Collectors.toSet());
  }


}
