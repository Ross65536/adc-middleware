package pt.inesctec.adcauthmiddleware.adc;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resources.RearrangementLoader;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;

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
