package pt.inesctec.adcauthmiddleware.adc;

public final class RepertoireConstants {
    /**
     * The repertoire's ID field name.
     */
    public static final String ID_FIELD = "repertoire_id";

    /**
     * The repertoire's study ID field name (the parent resource's ID). The comma separator is hardcoded in other parts of code.
     */
    public static final String UMA_ID_FIELD = "study.study_id";

    /**
     * The repertoire's study title field name. The comma separator is hardcoded in other parts of code.
     */
    public static final String STUDY_TITLE_FIELD = "study.study_title";

    /**
     * The ADC document (JSON object) response's field name for the repertoire list.
     */
    public static final String RESPONSE_FILTER_FIELD = "Repertoire";

    /**
     * The repertoire's study first fragment of the field. Used by JSON parser. In the code 2 levels are specified for the study ID field.
     */
    public static final String STUDY_BASE = "study";

    /**
     * The repertoire's study's study ID second fragment name.
     */
    public static final String STUDY_ID_BASE = "study_id";

    /**
     * The repertoire's study's study title second fragment name.
     */
    public static final String STUDY_TITLE_BASE = "study_title";

    /**
     * Database {@link pt.inesctec.adcauthmiddleware.db.models.AdcFieldType} name.
     */
    public static final String DB_FIELDTYPE = "repertoire";
}
