package pt.inesctec.adcauthmiddleware.adc.resources;

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
import pt.inesctec.adcauthmiddleware.controllers.SpringUtils;
import pt.inesctec.adcauthmiddleware.db.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.UmaState;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

/**
 * Base class for processing the output of an ADC Resource (Repertoires, Rearrangements...)
 * Deals with a single ADC resource
 */
public abstract class AdcResource {
    protected FieldClass fieldClass;

    protected AdcClient adcClient;
    protected DbService dbService;
    protected CsvConfig csvConfig;

    protected UmaState umaState = new UmaState();

    public AdcResource(FieldClass fieldClass, AdcClient adcClient, DbService dbService, CsvConfig csvConfig) {
        this.fieldClass = fieldClass;
        this.adcClient = adcClient;
        this.dbService = dbService;
        this.csvConfig = csvConfig;
    }

    /**
     * Abstract Function to be implemented by the ADC Resource.
     * Must be implemented to return a Set of UMA ids that identify this AdcResource with their UMA ID in the
     * Authorization service.
     *
     * @return Set
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
        umaState.setUmaIds(this.getUmaIds());
        umaState.setScopes(this.getUmaScopes());
        umaState.setResources(umaFlow.execute(bearerToken, umaState.getUmaIds(), umaState.getScopes()));
        umaState.isEnabled();
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
}
