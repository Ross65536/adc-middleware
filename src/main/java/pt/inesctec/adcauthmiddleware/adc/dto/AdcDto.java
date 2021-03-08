package pt.inesctec.adcauthmiddleware.adc.dto;

import java.util.List;
import java.util.Set;

import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.StudyMappings;
import pt.inesctec.adcauthmiddleware.db.repository.StudyMappingsRepository;
import pt.inesctec.adcauthmiddleware.db.services.DbService;

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

    // UMA Scopes for this resource
    protected Set<AccessScope> scopes;

    // List of UMA Field Accessibility mappings for the study this ADC resource belongs to
    protected List<StudyMappings> fieldMappings;

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
    protected abstract void setupUma() throws Exception;
}
