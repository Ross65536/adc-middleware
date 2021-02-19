package pt.inesctec.adcauthmiddleware.adc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.resources.RearrangementSet;
import pt.inesctec.adcauthmiddleware.adc.resources.RepertoireSet;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementModel;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireModel;
import pt.inesctec.adcauthmiddleware.adc.models.internal.AdcFacetsResponse;
import pt.inesctec.adcauthmiddleware.adc.models.internal.AdcIdsResponse;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

/**
 * Client for makings requests to an ADC compliant repository.
 */
@Component
public class AdcClient {

    private final AdcConfiguration adcConfig;

    public AdcClient(AdcConfiguration adcConfig) {
        this.adcConfig = adcConfig;
    }

    /**
     * Validate JAX assertions for list elements.
     *
     * @param resources the source list
     * @param <T>       type
     * @return the same list
     * @throws Exception on JAX assertion failure
     */
    private static <T> List<T> listPostConditions(List<T> resources) throws Exception {
        Utils.assertNotNull(resources);
        Utils.jaxValidateList(resources);

        return resources;
    }

    /**
     * Process a POST's facets response to obtain the set of values.
     *
     * @param facets      the facets response.
     * @param facetsField the facets field used in the query.
     * @return the set of values for the field.
     * @throws Exception when a facets response object doesn't contain the facets field (incorrect response).
     */
    private static Set<String> processStringFacets(List<Map<String, Object>> facets, String facetsField) throws Exception {
        Utils.assertNotNull(facets);
        CollectionsUtils.assertMapListContainsKeys(facets, facetsField);

        return facets.stream().filter(facet -> {
            var count = (Integer) facet.get("count");
            return count > 0;
        }).map(facet -> (String) facet.get(facetsField)).collect(Collectors.toSet());
    }

    /**
     * Access any repository endpoint.
     *
     * @param path the subpath which will be added to the one in the configuration
     * @return the response byte stream
     * @throws IOException          on error
     * @throws InterruptedException on error
     */
    public InputStream getResource(String path) throws IOException, InterruptedException {
        final URI uri = this.getResourceServerPath(path);

        var request = new HttpRequestBuilderFacade().getJson(uri).build();

        return HttpFacade.makeExpectJsonAsStreamRequest(request);
    }

    /**
     * GET /v1/repertoire/:id.
     *
     * @param repertoireId the repertoire's ID (repertoire_id)
     * @return repertoire byte stream
     * @throws IOException          on error
     * @throws InterruptedException on error
     */
    public InputStream getRepertoireAsStream(String repertoireId)
            throws IOException, InterruptedException {
        final URI uri = this.getResourceServerPath("repertoire", repertoireId);

        var request = new HttpRequestBuilderFacade().getJson(uri).build();

        return HttpFacade.makeExpectJsonAsStreamRequest(request);
    }

    /**
     * GET /v1/rearrangement/:id.
     *
     * @param rearrangementId the rearrangement's ID (sequence_id)
     * @return rearrangement byte stream
     * @throws IOException          on error
     * @throws InterruptedException on error
     */
    public InputStream getRearrangementAsStream(String rearrangementId)
            throws IOException, InterruptedException {
        final URI uri = this.getResourceServerPath("rearrangement", rearrangementId);

        var request = new HttpRequestBuilderFacade().getJson(uri).build();

        return HttpFacade.makeExpectJsonAsStreamRequest(request);
    }

    /**
     * GET /v1/rearrangement/:id, but the response is parsed.
     *
     * @param rearrangementId the rearrangement's ID (sequence_id)
     * @return the rearrangement model. empty list when ID not found, size 1 when found.
     * @throws Exception on error
     */
    public List<RearrangementModel> getRearrangement(String rearrangementId) throws Exception {
        final URI uri = this.getResourceServerPath("rearrangement", rearrangementId);
        var request = new HttpRequestBuilderFacade().getJson(uri).build();
        var rearrangements =
                HttpFacade.makeExpectJsonRequest(request, AdcIdsResponse.class).getRearrangements();

        listPostConditions(rearrangements);
        return rearrangements;
    }

    /**
     * POST /v1/repertoire.
     *
     * @param adcRequest the user's ADC request
     * @return the matching repertoires byte stream
     * @throws Exception on error
     */
    public InputStream searchRepertoiresAsStream(AdcSearchRequest adcRequest) throws Exception {
        Preconditions.checkArgument(adcRequest.isJsonFormat());

        var request = this.buildSearchRequest("repertoire", adcRequest);
        return HttpFacade.makeExpectJsonAsStreamRequest(request);
    }

    /**
     * POST /v1/rearrangement.
     *
     * @param adcRequest the user's ADC request
     * @return the matching rearrangements byte stream
     * @throws Exception on error
     */
    public InputStream searchRearrangementsAsStream(AdcSearchRequest adcRequest) throws Exception {
        Preconditions.checkArgument(adcRequest.isJsonFormat());

        var request = this.buildSearchRequest("rearrangement", adcRequest);
        return HttpFacade.makeExpectJsonAsStreamRequest(request);
    }

    /**
     * POST /v1/repertoire. But the response is parsed into models.
     *
     * @param adcRequest the user's ADC request
     * @return the matching repertoire models.
     * @throws Exception on error
     */
    public List<RepertoireModel> getRepertoireModel(AdcSearchRequest adcRequest) throws Exception {
        Preconditions.checkArgument(adcRequest.getFacets() == null);
        Preconditions.checkArgument(adcRequest.isJsonFormat());

        var request = this.buildSearchRequest("repertoire", adcRequest);
        var repertoires =
                HttpFacade.makeExpectJsonRequest(request, AdcIdsResponse.class).getRepertoires();

        return listPostConditions(repertoires);
    }

    /**
     * POST /v1/repertoire, but only the study IDs for the matching ADC query are returned.
     * Facets are used because it's a faster lookup over a regular search.
     *
     * @param adcRequest the user's ADC query
     * @return the set of study IDs
     * @throws Exception on error
     */
    public Set<String> getRepertoireStudyIds(AdcSearchRequest adcRequest) throws Exception {
        Preconditions.checkArgument(adcRequest.isJsonFormat());

        var idsQuery = adcRequest.queryClone().withFacets(RepertoireSet.UMA_ID_FIELD);
        var request = this.buildSearchRequest("repertoire", idsQuery);
        var facets = HttpFacade.makeExpectJsonRequest(
            request, AdcFacetsResponse.class
        ).getFacets();

        return processStringFacets(facets, RepertoireSet.UMA_ID_FIELD);
    }

    /**
     * POST /v1/rearrangement. But only the repertoire IDs for the matching ADC query are returned.
     * Facets are used because of the speedup the bring over the regular search.
     *
     * @param adcRequest the user's ADC query
     * @return the set of repertoire IDs
     * @throws Exception on error
     */
    public Set<String> getRearrangementRepertoireModel(AdcSearchRequest adcRequest) throws Exception {
        Preconditions.checkArgument(adcRequest.isJsonFormat());

        var idsQuery = adcRequest.queryClone().withFacets(RearrangementSet.REPERTOIRE_ID_FIELD);
        var request = this.buildSearchRequest("rearrangement", idsQuery);
        var facets = HttpFacade.makeExpectJsonRequest(request, AdcFacetsResponse.class).getFacets();

        return processStringFacets(facets, RearrangementSet.REPERTOIRE_ID_FIELD);
    }

    /**
     * Build HTTP request for an AdcSearchRequest.
     *
     * @param path             the subpath fragment
     * @param adcSearchRequest the user's ADC request
     * @return the request model
     * @throws JsonProcessingException on error
     */
    private HttpRequest buildSearchRequest(String path, AdcSearchRequest adcSearchRequest)
            throws JsonProcessingException {
        final URI uri = this.getResourceServerPath(path);
        return new HttpRequestBuilderFacade().postJson(uri, adcSearchRequest).expectJson().build();
    }

    /**
     * Appends the URL set in the configuration for the repository with the path parts.
     *
     * @param parts URL parts to append
     * @return the complete URL
     */
    private URI getResourceServerPath(String... parts) {
        final String basePath = adcConfig.getResourceServerUrl();
        return Utils.buildUrl(basePath, parts);
    }


}
