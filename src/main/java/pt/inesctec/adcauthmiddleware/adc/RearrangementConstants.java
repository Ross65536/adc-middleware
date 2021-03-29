package pt.inesctec.adcauthmiddleware.adc;

public final class RearrangementConstants {
    /**
     * The rearrangement's ID field name.
     */
    public static final String ID_FIELD = "sequence_id";

    /**
     * The rearrangement's repertoire ID field name (the parent resource's ID).
     */
    public static final String REPERTOIRE_ID_FIELD = "repertoire_id";

    /**
     * The ADC document (JSON object) response's field name for the rearrangement list.
     */
    public static final String RESPONSE_FILTER_FIELD = "Rearrangement";

    /**
     * Database {@link pt.inesctec.adcauthmiddleware.db.models.AdcFieldType} name
     */
    public static final String DB_FIELDTYPE = "rearrangement";
}
