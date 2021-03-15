package pt.inesctec.adcauthmiddleware.adc.resources;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

/**
 * Base class for processing the output of an ADC Resource (Repertoires, Rearrangements...)
 */
public abstract class AdcResourceLoader {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcResourceLoader.class);

    // Application's singletons
    protected AdcClient adcClient;
    protected DbService dbService;

    // List of UMA Field Accessibility mappings for the study this ADC resource belongs to
    protected List<String> fieldMappings;

    // UMA Resource the represents this entity in the UMA Service
    protected UmaResource umaResource;

    // Defines if this resource should trigger the UMA workflow.
    //  * If Disabled - Will map the resource to only output fields that have been defined as public
    //  * If Enabled  - Will follow the UMA workflow normally
    boolean umaEnabled = false;

    public AdcResourceLoader(AdcClient adcClient, DbService dbService) {
        this.adcClient  = adcClient;
        this.dbService  = dbService;
    }

    /**
     * Load UMA IDs that identify the resource in the UMA Service
     *
     * Used for single resource access
     *
     * @param adcId ID that identifies the resource in the ADC service
     * @return Set of UMA IDs
     */
    public abstract Set<String> loadUmaIds(String adcId) throws Exception;

    /**
     * Load UMA IDs that identify the resource in the UMA Service
     *
     * Used for ADC Search requests (multiple resources/no specific ID)
     *
     * @param adcSearch {@link AdcSearchRequest}
     * @return Set of UMA IDs
     */
    public abstract Set<String> loadUmaIds(AdcSearchRequest adcSearch) throws Exception;

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Must be implemented to return the UMA ID that identifies this resource in the Authorization service.
     *
     * @param bearerToken OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaFlow UmaFlow object
     * @param resourceState Current UMA states
     * @throws Exception when emitting a permission ticket or an internal error occurs.
     */
    public abstract void processUma(String bearerToken, UmaFlow umaFlow, ResourceState resourceState) throws Exception;

    /**
     * Loads ADC field mappings according to the defined settings in the UMA Service the Middleware's database
     *
     * @param umaConfig UMA configuration
     * @param resourceState Resource State Manager
     * @throws Exception on internal errors, i.e. database errors since this is method highly dependant on DB access
     */
    public void loadFieldMappings(UmaConfig umaConfig, ResourceState resourceState) throws Exception {
        // Iterate requested resources
        for(String umaId : resourceState.getUmaIds()) {
            List<String> scopes = new ArrayList<>();

            // Public Request
            if (!resourceState.isEnabled()) {
                scopes.add(umaConfig.getPublicScopeName());
            }
            // Protected Request
            else {
                // If it's a protected request but if for some reason it fails to determine the resource in the UMA service
                // Fallback to public fields
                if (resourceState.getResources().isEmpty() || !resourceState.getResources().containsKey(umaId)) {
                    Logger.info("Unable to determine resource with UMA ID {} - not present in UMA Service. Is database /synchronized?", umaId);
                    scopes.add(umaConfig.getPublicScopeName());
                } else {
                    scopes = new ArrayList<>(
                        resourceState.getResources().get(umaId).getUmaResource().getScopes()
                    );
                }
            }

            List<String> fields = this.dbService.getStudyMappingsRepository().findFieldNamesByUmaIdAndScopesIn(
                umaId, scopes
            );

            resourceState.getResources().get(umaId).setFieldMappings(fields);
        }
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
        var filter = new FieldsFilter(fieldMapper, resourceId);
        var mapper = AdcJsonDocumentParser.buildJsonMapper(response, responseFilterField, filter);
        return SpringUtils.buildJsonStream(mapper);
    }

    public boolean isUmaEnabled() {
        return umaEnabled;
    }

    public void setUmaEnabled(boolean umaEnabled) {
        this.umaEnabled = umaEnabled;
    }
}
