package pt.inesctec.adcauthmiddleware.adc.dto;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.StudyMappings;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

/**
 * Base class for processing the output of an ADC Resource (Repertoires, Rearrangements...)
 */
public abstract class AdcDto {
    // Application's singletons
    protected AdcClient adcClient;
    protected DbService dbService;

    // ID in the ADC service
    protected String adcId;

    // ID in the UMA service
    protected String umaId;

    // List of UMA Field Accessibility mappings for the study this ADC resource belongs to
    protected List<StudyMappings> fieldMappings;

    // Defines if this resource should trigger the UMA workflow.
    //  * If Disabled - Will map the resource to only output fields that have been defined as public
    //  * If Enabled  - Will follow the UMA workflow normally
    boolean umaEnabled = false;

    public AdcDto(AdcClient adcClient, DbService dbService) {
        this.adcClient  = adcClient;
        this.dbService  = dbService;
    }

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Must be implemented to return the UMA IDs(*) that identify this AdcResource in the Authorization service.
     * (*) Single value from a single Resource, multiple for ResourceSets
     *
     */
    public abstract void processUma(String bearerToken, UmaFlow umaFlow) throws Exception;

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Should be the final step in a controller and returns the ADC compliant response for the current entity,
     * filtered according to the User's permissions.
     *
     * @return ResponseEntity
     */
    public abstract ResponseEntity<StreamingResponseBody> response() throws Exception;

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

    public boolean isUmaEnabled() {
        return umaEnabled;
    }

    public void setUmaEnabled(boolean umaEnabled) {
        this.umaEnabled = umaEnabled;
    }
}
