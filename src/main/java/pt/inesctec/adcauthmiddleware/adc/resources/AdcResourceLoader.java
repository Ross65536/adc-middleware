package pt.inesctec.adcauthmiddleware.adc.resources;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.IFieldsFilter;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.db.models.AdcFieldType;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.utils.ThrowingFunction;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

/**
 * Base class for processing the output of an ADC Resource (Repertoires, Rearrangements...)
 */
public abstract class AdcResourceLoader {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcResourceLoader.class);

    protected AdcClient adcClient;
    protected DbService dbService;

    protected ResourceState resourceState = new ResourceState();
    protected String adcFieldTypeName;
    protected AdcFieldType adcFieldType;

    public AdcResourceLoader(String adcFieldTypeName, AdcClient adcClient, DbService dbService) {
        this.adcClient        = adcClient;
        this.dbService        = dbService;
        this.adcFieldTypeName = adcFieldTypeName;
    }

    /**
     * Base Load method.
     * Loads specific parameters on initialization.
     */
    public void load() {
        this.adcFieldType = this.dbService.getAdcFieldType(this.adcFieldTypeName);
    }

    /**
     * Used for single resource access.
     * Load UMA IDs that identify the resource in the UMA Service.
     * The Load method should establish the resource's UMA ID's along with the scopes.
     *
     * @param adcId ID that identifies the resource in the ADC service
     */
    public abstract void load(String adcId) throws Exception;

    /**
     * Used for ADC Search requests (multiple resources/no specific ID).
     * Load UMA IDs that identify the resource in the UMA Service.
     * The Load method should establish the resource's UMA ID's along with the scopes.
     *
     * @param adcSearch {@link AdcSearchRequest}
     */
    public abstract void load(AdcSearchRequest adcSearch) throws Exception;

    /**
     * Load scopes for resource.
     *
     * @return Possible set of UMA scopes for the specific resource
     * @throws Exception on internal errors
     */
    protected Set<String> loadScopes() throws Exception {
        return this.dbService.getStudyMappingsRepository().findScopesByUmaIds(
            this.resourceState.getUmaIds(), this.adcFieldType
        );
    }

    /**
     * Load scopes for multiple resources returned by an AdcSearchRequest.
     *
     * @param adcSearch {@link AdcSearchRequest}
     * @return Possible set of UMA scopes for the specific resource
     * @throws Exception on internal errors
     */
    protected Set<String> loadScopes(AdcSearchRequest adcSearch) throws Exception {
        Set<String> requestedFields;

        if (adcSearch.isFacetsSearch()) {
            requestedFields = Set.of(adcSearch.getFacets());
        } else {
            requestedFields = adcSearch.getRequestedFields();
        }

        // If specific fields were requested, get permissions just for those fields
        if (requestedFields.isEmpty()) {
            return this.dbService.getStudyMappingsRepository().findScopesByUmaIds(
                this.resourceState.getUmaIds(), this.adcFieldType
            );
        }

        // If no specific fields, get all mappings for this resource type (defined by adcFieldType).
        // More time consuming but probably the most common request too
        return this.dbService.getStudyMappingsRepository().findScopesByUmaIdsAndByFields(
            this.resourceState.getUmaIds(), this.adcFieldType, requestedFields
        );
    }

    /**
     * Executes the UMA Workflow.
     * Throws a permission ticket if no Bearer is present;
     * Collects {@link pt.inesctec.adcauthmiddleware.uma.dto.UmaResource} that identify
     * the requested resources in the Authorization service if a RPT is provided.
     *
     * @param bearerToken OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaFlow UmaFlow object
     * @throws Exception when emitting a permission ticket or an internal error occurs.
     */
    public void processUma(String bearerToken, UmaFlow umaFlow) throws Exception {
        if (resourceState.getScopes().isEmpty()) {
            String error = "Internal Error - Error while processing UMA: No scopes were found.";
            Logger.error(error);
            throw new Exception(error);
        }

        resourceState.setUmaEnabled(true);
        resourceState.setFromUmaResources(umaFlow.execute(
            bearerToken, resourceState.getUmaIds(), resourceState.getScopes()
        ));
    }

    /**
     * Loads ADC field mappings according to the defined settings in the UMA Service the Middleware's database
     * and sets them into the current resources.
     * <p>
     *     If UMA is enabled: Will retrieve the Fields Mappings that correspond to the
     *     scopes the user has access to, and sets them to the current resources;
     * </p>
     * <p>
     *     If UMA is disabled: Only Retrieve Fields that have been mapped to the "public" scope.
     * </p>
     * Note: the nomenclature for the public scope may be set in the .configuration file through the attribute
     * <pre>uma.publicScopeName=public</pre>
     *
     * @param umaConfig UMA configuration
     * @throws Exception on internal errors, i.e. database errors since this is method highly dependant on DB access
     */
    public void loadFieldMappings(UmaConfig umaConfig) throws Exception {
        // Iterate requested resources
        for (String umaId : resourceState.getUmaIds()) {
            List<String> scopes = new ArrayList<>();

            // Determine scope access
            // * Public Request
            if (!resourceState.isUmaEnabled()) {
                scopes.add(umaConfig.getPublicScopeName());
            } else {
                // * Protected Request - Missing Resource
                // If for some reason it fails to determine the resource in the UMA service
                // then fallback to public fields
                if (resourceState.getResources().isEmpty() || !resourceState.getResources().containsKey(umaId)) {
                    Logger.warn("Unable to determine resource with UMA ID {} - not present in UMA Service. Is database /synchronized?", umaId);
                    scopes.add(umaConfig.getPublicScopeName());
                } else {
                    // * Protected Request - Default Behaviour
                    // If it succeeds in getting the resource, determine the scopes accessible to the user
                    // according to the UMA Service as determined by AdcResourceLoader::processUma()
                    scopes = new ArrayList<>(
                        resourceState.getResources().get(umaId).getUmaResource().getScopes()
                    );
                }
            }

            List<String> fields = this.dbService.getStudyMappingsRepository().findMappings(
                umaId, this.adcFieldType, scopes
            );

            // If the resource wasn't returned by the UMA Service, create a Resource with no UMA State
            if (!resourceState.getResources().containsKey(umaId)) {
                resourceState.getResources().put(umaId, new AdcResource());
            }

            resourceState.getResources().get(umaId).setFieldMappings(fields);
        }
    }

    /**
     * Abstract Function to be implemented by the resource loader.
     * Should be the final step in a controller and returns the ADC compliant response for the current entity,
     * filtered according to the User's permissions.
     *
     * @param adcId Single ADC resource ID
     * @return ResponseEntity
     */
    public abstract ResponseEntity<StreamingResponseBody> response(String adcId) throws Exception;

    /**
     * Abstract Function to be implemented by the resource loader.
     * Should be the final step in a controller and returns the ADC compliant response for the current entity,
     * filtered according to the User's permissions.
     *
     * @param adcSearch for ADC search requests
     * @return ResponseEntity
     */
    public abstract ResponseEntity<StreamingResponseBody> response(AdcSearchRequest adcSearch) throws Exception;

    /**
     * From a list of ADC resources, determine based on their fieldMappings and the requested Facet,
     * the list of resource IDs that identify the Facets accessible by the user.
     * Example:
     *   For /repertoire it shall return a List of study.study_id
     *   For /rearrangement it shall return a List of repertoire_id
     *
     * @param adcSearchRequest user's request
     * @param adcResources determined resource list from the request
     * @param resourceGetter function that returns the collection of resource IDs given an UMA ID.
     * @return List of resource identifying values
     */
    public static List<String>  loadFacetIds(
        AdcSearchRequest adcSearchRequest,
        Map<String, AdcResource> adcResources,
        Function<String, Set<String>> resourceGetter
    ) {
        String requestedFacet = adcSearchRequest.getFacets();

        List<String> validResources = new ArrayList<>();

        for (var resource : adcResources.entrySet()) {
            if (resource.getValue().getFieldMappings().contains(requestedFacet)) {
                var resourceId = resourceGetter.apply(resource.getKey());
                validResources.addAll(resourceId);
            }
        }

        return validResources;
    }

    /**
     * Build JSON streaming, filtered response.
     *
     * @param resourceId          name of the field that identifies this UMA resource in the AdcRequest response
     * @param responseFilterField name of the JSON object in the AdcRequest response that contains this Resource
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

        //var stringBody = CharStreams.toString(new InputStreamReader(response, Charsets.UTF_8));
        //System.out.println(stringBody);

        var filter = new FieldsFilter(fieldMapper, resourceId);
        var mapper = AdcJsonDocumentParser.buildJsonMapper(response, responseFilterField, filter);

        return SpringUtils.buildJsonStream(mapper);
    }

    /**
     * Core facets request.
     *
     * @param resourceId       name of the field that identifies the resource.
     * @param adcSearch        the user's ADC query.
     * @param adcRequest       the request function
     * @param resourceIds      the permitted list of resource IDs for facets.
     * @return the streamed facets.
     * @throws Exception on error.
     */
    public static ResponseEntity<StreamingResponseBody> responseFilteredFacets(
        AdcSearchRequest adcSearch,
        String resourceId,
        ThrowingFunction<AdcSearchRequest, InputStream, Exception> adcRequest,
        List<String> resourceIds
    ) throws Exception {
        adcSearch.withFieldIn(resourceId, resourceIds);

        var is = SpringUtils.catchForwardingError(() -> adcRequest.apply(adcSearch));

        IFieldsFilter filter = FieldsFilter.OpenFilter;

        var mapper = AdcJsonDocumentParser.buildJsonMapper(is, AdcConstants.ADC_FACETS, filter);
        return SpringUtils.buildJsonStream(mapper);
    }
}
