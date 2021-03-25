package pt.inesctec.adcauthmiddleware.adc;

import java.util.Set;

public final class AdcConstants {
    /**
     * The UMA type value used when creating a UMA resource.
     */
    public static final String UMA_STUDY_TYPE = "study";
    /**
     * The UMA type value used when 'deleting' a resource.
     */
    public static final String UMA_DELETED_STUDY_TYPE = "deleted_study";

    /**
     * The set of all used and mandatory repertoire and rearrangement fields. Used for the CSV configuration file validation.
     */
    public static final Set<String> AllUsedFields = Set.of(
        RepertoireConstants.UMA_ID_FIELD,
        RepertoireConstants.STUDY_TITLE_FIELD,
        RepertoireConstants.ID_FIELD,
        RearrangementConstants.ID_FIELD
    );

    /**
     * The ADC document info parameter (passed to the user's response unmodified in some cases).
     */
    public static final String ADC_INFO = "Info";
    /**
     * The ADC document (JSON object) response's field name for a facets search's list of counts.
     */
    public static final String ADC_FACETS = "Facet";
    /**
     * The ADC field separator.
     */
    public static final String ADC_FIELD_SEPARATOR = ".";
}
