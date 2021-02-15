package pt.inesctec.adcauthmiddleware.adc.resources;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.IFieldsFilter;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.controllers.SpringUtils;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;
import pt.inesctec.adcauthmiddleware.utils.ThrowingFunction;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

/**
 * Base class for processing the output of ADC Resources (Repertoires, Rearrangements...)
 */
public abstract class AdcResource {
    protected FieldClass fieldClass;
    protected AdcSearchRequest adcSearch;

    protected AdcClient adcClient;
    protected DbRepository dbRepository;
    protected CsvConfig csvConfig;

    // UMA Status variables. Relevant for UMA related requests
    protected boolean umaEnabled = false;
    protected Set<String> umaScopes;
    protected Set<String> umaIds;
    protected List<UmaResource> umaResources;

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Must be implemented to return a Set of UMA ids that relate the AdcResource with their UMA ID in the Authorization
     * infrastructure.
     *
     * @return Set
     */
    public abstract Set<String> getUmaIds() throws Exception;

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Should be the final step in a controller and returns the ADC compliant response for the current entity,
     * filtered according to the User's permissions.
     *
     * @return ResponseEntity
     */
    public abstract ResponseEntity<StreamingResponseBody> response() throws Exception;

    public AdcResource(FieldClass fieldClass, AdcSearchRequest adcSearch, AdcClient adcClient, DbRepository dbRepository, CsvConfig csvConfig) {
        this.fieldClass = fieldClass;
        this.adcSearch = adcSearch;
        this.adcClient = adcClient;
        this.dbRepository = dbRepository;
        this.csvConfig = csvConfig;
    }

    /**
     * Sets the current AdcResource as being protected by UMA.
     * Any output by the response() method will be controlled by the User's permissions
     *
     * @param bearerToken OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaFlow UmaFlow object
     * @throws Exception according to the UMA workflow
     */
    public void enableUma(String bearerToken, UmaFlow umaFlow) throws Exception {
        this.umaIds       = this.getUmaIds();
        this.umaScopes    = this.getUmaScopes(this.fieldClass);
        this.umaResources = umaFlow.execute(bearerToken, umaIds, umaScopes);
        this.umaEnabled   = true;
    }

    /**
     * Returns the UMA scopes for the fields in this Request.
     * The considered parameters are: "facets", "fields", "include_fields", and "filters".
     * Filters operators can reference a field for the search and these are the fields considered.
     *
     * @param fieldClass the resource type
     * @return the UMA scopes.
     */
    protected Set<String> getUmaScopes(FieldClass fieldClass) {
        final Set<String> requestedFields = this.adcSearch.isFacetsSearch()
            ? Set.of(this.adcSearch.getFacets())
            : this.adcSearch.getRequestedFields(this.fieldClass, this.csvConfig);

        final Set<String> filtersFields = this.adcSearch.getRequestedFilterFields();
        final Set<String> allConsideredFields = Sets.union(requestedFields, filtersFields);

        // empty set returned means only public fields requested
        return csvConfig.getUmaScopes(this.fieldClass, allConsideredFields);
    }

    protected Function<String, Set<String>> setupPublicFieldMapper(FieldClass fieldClass, String resourceId) {
        final Set<String> allRequestedFields = this.adcSearch.getRequestedFields(fieldClass, csvConfig);
        final Set<String> filtersFields      = this.adcSearch.getRequestedFilterFields();

        if (!allRequestedFields.contains(resourceId)) {
            this.adcSearch.addField(resourceId);
        }

        return umaId -> {
            Set<String> publicFields = csvConfig.getPublicFields(fieldClass);

            // Prevent indirect queries to inaccessible fields
            // Don't return resources where the access level does not match the one in the
            // filters, in order to avoid information leaks
            if (Sets.difference(filtersFields, publicFields).isEmpty()) {
                return Sets.intersection(publicFields, allRequestedFields);
            }

            Set<String> emptySet = ImmutableSet.of();
            return emptySet;
        };
    }

    /**
     * Setup the ADC request and build the mapper for a regular listing.
     *
     * @param fieldClass   the resource type.
     * @param resourceId   the resource's ID field.
     * @return the UMA ID to permitted fields mapper
     */
    protected Function<String, Set<String>> setupFieldMapper(FieldClass fieldClass, String resourceId) {
        final Set<String> allRequestedFields = this.adcSearch.getRequestedFields(fieldClass, csvConfig);
        final Set<String> filtersFields      = this.adcSearch.getRequestedFilterFields();

        if (!allRequestedFields.contains(resourceId)) {
            this.adcSearch.addField(resourceId);
        }

        return UmaUtils.buildFieldMapper(umaResources, fieldClass, csvConfig).andThen(
            fields -> {
                // Prevent indirect queries to inaccessible fields
                // Don't return resources where the access level does not match the one in the
                // filters, in order to avoid information leaks
                if (Sets.difference(filtersFields, fields).isEmpty()) {
                    return fields;
                }

                Set<String> emptySet = ImmutableSet.of();
                return emptySet;
            }).andThen(set -> Sets.intersection(set, allRequestedFields));
    }

    /**
     * Core facets request.
     *
     * @param adcSearch        the user's ADC query.
     * @param resourceId       the resource's ID field
     * @param adcRequest       the request function
     * @param resourceIds      the permitted list of resource IDs for facets.
     * @param isProtected      whether the request made is protected or public.
     * @return the streamed facets.
     * @throws Exception on error.
     */
    public static ResponseEntity<StreamingResponseBody> responseFilteredFacets(
        AdcSearchRequest adcSearch,
        String resourceId,
        ThrowingFunction<AdcSearchRequest, InputStream, Exception> adcRequest,
        List<String> resourceIds,
        boolean isProtected
    ) throws Exception {
        boolean filterResponse = false;

        if (isProtected) { // non public facets field
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
    public static ResponseEntity<StreamingResponseBody> responseFilteredJson(
        String resourceId,
        String responseFilterField,
        Function<String, Set<String>> fieldMapper,
        ThrowingSupplier<InputStream, Exception> adcRequest
    ) throws Exception {
        var response = SpringUtils.catchForwardingError(adcRequest);
        var filter = new FieldsFilter(fieldMapper, resourceId);
        var mapper = AdcJsonDocumentParser.buildJsonMapper(response, responseFilterField, filter);
        return SpringUtils.buildJsonStream(mapper);
    }

    /**
     * Build TSV streaming, filtered, response.
     *
     * @param resourceId          the resource's ID fields
     * @param responseFilterField the response's field where the resources are set
     * @param fieldMapper         the ID to granted fields mapper
     * @param adcRequest          the ADC request producer.
     * @param headerFields        the TSV header fields which will be the response's first line.
     * @return streaming response
     * @throws Exception on error
     */
    public static ResponseEntity<StreamingResponseBody> responseFilteredTsv(
        String resourceId,
        String responseFilterField,
        Function<String, Set<String>> fieldMapper,
        ThrowingSupplier<InputStream, Exception> adcRequest,
        Map<String, FieldType> headerFields)
        throws Exception {
        var response = SpringUtils.catchForwardingError(adcRequest);
        var filter = new FieldsFilter(fieldMapper, resourceId);
        var mapper = AdcJsonDocumentParser.buildTsvMapper(
            response, responseFilterField, filter, headerFields
        );
        return SpringUtils.buildTsvStream(mapper);
    }
}
