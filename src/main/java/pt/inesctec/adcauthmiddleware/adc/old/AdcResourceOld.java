package pt.inesctec.adcauthmiddleware.adc.old;

import java.io.InputStream;
import java.util.Set;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.UmaStateOld;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

/**
 * Base class for processing the output of an ADC Resource (Repertoires, Rearrangements...)
 * Deals with a single ADC resource
 */
public abstract class AdcResourceOld {
    protected FieldClass fieldClass;

    protected AdcClient adcClient;
    protected DbService dbService;
    protected CsvConfig csvConfig;

    protected UmaStateOld umaStateOld = new UmaStateOld();

    public AdcResourceOld(FieldClass fieldClass, AdcClient adcClient, DbService dbService, CsvConfig csvConfig) {
        this.fieldClass = fieldClass;
        this.adcClient = adcClient;
        this.dbService = dbService;
        this.csvConfig = csvConfig;
    }

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Must be implemented to return the UMA IDs(*) that identify this AdcResource in the Authorization service.
     * (*) Single value from a single Resource, multiple for ResourceSets
     *
     * @return Set of UMA IDs
     */
    protected abstract Set<String> getUmaIds() throws Exception;

    /**
     * Returns the UMA scopes for the fields in this Request.
     * The considered parameters are: "facets", "fields", "include_fields", and "filters".
     * Filters operators can reference a field for the search and these are the fields considered.
     *
     * @return the UMA scopes.
     */
    protected abstract Set<String> getUmaScopes();

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Should be the final step in a controller and returns the ADC compliant response for the current entity,
     * filtered according to the User's permissions.
     *
     * @return ResponseEntity
     */
    public abstract ResponseEntity<StreamingResponseBody> response() throws Exception;

    /**
     * Sets the current AdcResource as being protected by UMA.
     * Any output by the response() method will be controlled by the User's permissions
     *
     * @param bearerToken OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaFlow UmaFlow object
     * @throws Exception according to the UMA workflow
     */
    public void enableUma(String bearerToken, UmaFlow umaFlow) throws Exception {
        umaStateOld.setEnabled(true);
        umaStateOld.setUmaIds(this.getUmaIds());
        umaStateOld.setScopes(this.getUmaScopes());
        umaStateOld.setResources(umaFlow.execute(bearerToken, umaStateOld.getUmaIds(), umaStateOld.getScopes()));
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
}
