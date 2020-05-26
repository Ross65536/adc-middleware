package pt.inesctec.adcauthmiddleware.adc;

import java.util.Set;

public final class AdcConstants {
  public static final String UMA_STUDY_TYPE = "study";

  public static final String REPERTOIRE_STUDY_ID_FIELD = "study.study_id";
  public static final String REPERTOIRE_STUDY_TITLE_FIELD = "study.study_title";
  public static final String REPERTOIRE_REPERTOIRE_ID_FIELD = "repertoire_id";
  public static final String REARRANGEMENT_REPERTOIRE_ID_FIELD = REPERTOIRE_REPERTOIRE_ID_FIELD;
  public static final String REARRANGEMENT_REARRANGEMENT_ID_FIELD = "rearrangement_id";

  public static final Set<String> AllUsedFields =
      Set.of(
          REPERTOIRE_STUDY_ID_FIELD,
          REPERTOIRE_STUDY_TITLE_FIELD,
          REPERTOIRE_REPERTOIRE_ID_FIELD,
          REARRANGEMENT_REARRANGEMENT_ID_FIELD);

  public static final String REPERTOIRE_RESPONSE_FILTER_FIELD = "Repertoire";
  public static final String REARRANGEMENT_RESPONSE_FILTER_FIELD = "Rearrangement";
  public static final String ADC_INFO = "Info";
  public static final String ADC_FACETS = "Facet";
}
