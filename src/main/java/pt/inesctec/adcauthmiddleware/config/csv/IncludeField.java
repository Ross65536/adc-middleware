package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

/**
 * Models the configuration CSV's 'include_fields' column. Corresponds to the ADC query parameter with the same name.
 */
public enum IncludeField {
  @JsonProperty("miairr")
  MIAIRR,
  @JsonProperty("airr-core")
  AIRR_CORE,
  @JsonProperty("airr-schema")
  AIRR_SCHEMA;


  private static final Set<IncludeField> MiairrScoping = Set.of(MIAIRR);
  private static final Set<IncludeField> AirrCoreScoping = Set.of(MIAIRR, AIRR_CORE);
  private static final Set<IncludeField> AirrSchemaScoping = Set.of(MIAIRR, AIRR_CORE, AIRR_SCHEMA);

  public static Set<IncludeField> getIncludeFieldScoping(IncludeField includeField) {
    switch (includeField) {
      case MIAIRR:
        return MiairrScoping;
      case AIRR_CORE:
        return AirrCoreScoping;
      case AIRR_SCHEMA:
        return AirrSchemaScoping;
      default:
        return Set.of();
    }
  }
}
