package pt.inesctec.adcauthmiddleware.controllers;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.HttpException;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.IFieldsFilter;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Delayer;
import pt.inesctec.adcauthmiddleware.utils.ThrowingFunction;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

@RequestMapping("${app.airrBasepath}")
public abstract class AdcController {
    @Autowired
    protected AppConfig appConfig;
    @Autowired
    protected CsvConfig csvConfig;
    @Autowired
    protected UmaFlow umaFlow;
    @Autowired
    protected UmaClient umaClient;
    @Autowired
    protected AdcClient adcClient;
    @Autowired
    protected DbRepository dbRepository;

    protected static org.slf4j.Logger Logger;
    protected Delayer repertoiresDelayer;
    protected Delayer rearrangementsDelayer;

    /**
     * Validate that the user's ADC query is semantically correct. Also enforces disabled features as set in the configuration.
     *
     * @param adcSearch  the user's ADC query
     * @param fieldClass the resource type
     * @param tsvEnabled whether TSV is enabled for the considered endpoint.
     * @throws HttpException on validation error
     */
    protected void validateAdcSearch(
        AdcSearchRequest adcSearch, FieldClass fieldClass, boolean tsvEnabled) throws HttpException {

        if (adcSearch.isFacetsSearch() && !this.appConfig.isFacetsEnabled()) {
            throw SpringUtils.buildHttpException(
                HttpStatus.NOT_IMPLEMENTED,
                "Invalid input JSON: 'facets' support for current repository not enabled");
        }

        if (adcSearch.getFilters() != null && !this.appConfig.isAdcFiltersEnabled()) {
            throw SpringUtils.buildHttpException(
                HttpStatus.NOT_IMPLEMENTED,
                "Invalid input JSON: 'filters' support for current repository not enabled");
        }

        var filtersBlacklist = this.appConfig.getFiltersOperatorsBlacklist();
        Set<String> actualFiltersOperators = adcSearch.getFiltersOperators();
        Sets.SetView<String> operatorDiff = Sets.intersection(filtersBlacklist, actualFiltersOperators);
        if (!operatorDiff.isEmpty()) {
            throw SpringUtils.buildHttpException(
                HttpStatus.NOT_IMPLEMENTED,
                "Invalid input JSON: 'filters' operators: "
                    + CollectionsUtils.toString(operatorDiff)
                    + " are blacklisted");
        }

        final boolean isTsv = !adcSearch.isJsonFormat();
        if (isTsv && !tsvEnabled) {
            throw SpringUtils.buildHttpException(
                HttpStatus.UNPROCESSABLE_ENTITY, "TSV format not enabled for this endpoint");
        }

        var fieldTypes = this.csvConfig.getFieldsTypes(fieldClass);
        var requestedFields = adcSearch.getRequestedFields(FieldClass.REARRANGEMENT, this.csvConfig);
        try {
            AdcSearchRequest.validate(adcSearch, fieldTypes, requestedFields);
        } catch (AdcException e) {
            throw SpringUtils.buildHttpException(
                HttpStatus.UNPROCESSABLE_ENTITY, "Invalid input JSON: " + e.getMessage());
        }
    }

    /**
     * From the UMA resource list and scopes obtain the list of resource IDs that can be safely processed for the resource type.
     *
     * @param umaResources the UMA resources and scopes.
     * @param umaScopes    the UMA scopes that the user must have access to for the resource, otherwise the resource is not considered.
     * @param umaIdGetter  function that returns the collection of resource IDs given the UMA ID.
     * @return the filtered collection of resource IDs.
     */
    protected List<String> calcValidFacetsResources(
        Collection<UmaResource> umaResources,
        Set<String> umaScopes,
        Function<String, Set<String>> umaIdGetter) {
        return umaResources.stream()
            .filter(resource -> !Sets.intersection(umaScopes, resource.getScopes()).isEmpty())
            .map(resource -> umaIdGetter.apply(resource.getUmaResourceId()))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Core facets request.
     *
     * @param adcSearch        the user's ADC query.
     * @param resourceId       the resource's ID field
     * @param adcRequest       the request function
     * @param resourceIds      the permitted list of resource IDs for facets.
     * @param restrictedAccess whether the request made is protected or public.
     * @return the streamed facets.
     * @throws Exception on error.
     */
    protected ResponseEntity<StreamingResponseBody> facetsRequest(
        AdcSearchRequest adcSearch,
        String resourceId,
        ThrowingFunction<AdcSearchRequest, InputStream, Exception> adcRequest,
        List<String> resourceIds,
        boolean restrictedAccess)
        throws Exception {

        boolean filterResponse = false;
        if (restrictedAccess) { // non public facets field
            adcSearch.withFieldIn(resourceId, resourceIds);
            filterResponse = resourceIds.isEmpty();
        }

        var is = SpringUtils.catchForwardingError(() -> adcRequest.apply(adcSearch));
        // will only perform whitelist filtering if rpt grants access to nothing, for partial access the
        // backend must perform the filtering
        IFieldsFilter filter = filterResponse ? FieldsFilter.BlockingFilter : FieldsFilter.OpenFilter;
        var mapper = AdcJsonDocumentParser.buildJsonMapper(is, AdcConstants.ADC_FACETS, filter);
        return SpringUtils.buildJsonStream(mapper);
    }

    /**
     * Build JSON streaming, filtered response.
     *
     * @param resourceId          the resource's ID fields
     * @param responseFilterField the response's field where the resources are set
     * @param fieldMapper         the ID to granted fields mapper
     * @param adcRequest          the ADC request producer.
     * @return streaming response
     * @throws Exception on error
     */
    protected static ResponseEntity<StreamingResponseBody> buildFilteredJsonResponse(
        String resourceId,
        String responseFilterField,
        Function<String, Set<String>> fieldMapper,
        ThrowingSupplier<InputStream, Exception> adcRequest)
        throws Exception {
        var response = SpringUtils.catchForwardingError(adcRequest);
        var filter = new FieldsFilter(fieldMapper, resourceId);
        var mapper = AdcJsonDocumentParser.buildJsonMapper(response, responseFilterField, filter);
        return SpringUtils.buildJsonStream(mapper);
    }

    /**
     * Function to obtain the unique study UMA IDs that correspond to the user's rearrangement ADC query search.
     *
     * @param idsQuery the ADC query
     * @return the UMA IDs
     * @throws Exception on error
     */
    protected Set<String> getRepertoireStudyIds(AdcSearchRequest idsQuery) throws Exception {
        return this.adcClient.getRepertoireStudyIds(idsQuery).stream()
            .map(id -> this.dbRepository.getStudyUmaId(id))
            .collect(Collectors.toSet());
    }

    /**
     * Obtain the fields and their types from the user's regular ADC request. Should check previously that the request is not facets.
     *
     * @param request    The user's ADC query
     * @param fieldClass the resource type
     * @return the fields and types
     */
    protected Map<String, FieldType> getRegularSearchRequestedFieldsAndTypes(
        AdcSearchRequest request, FieldClass fieldClass) {
        var requestedFields = request.getRequestedFields(fieldClass, this.csvConfig);
        Map<String, FieldType> allFields = this.csvConfig.getFieldsTypes(fieldClass);
        return CollectionsUtils.intersectMapWithSet(allFields, requestedFields);
    }
}
