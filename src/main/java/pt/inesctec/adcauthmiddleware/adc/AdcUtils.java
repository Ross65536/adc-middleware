package pt.inesctec.adcauthmiddleware.adc;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class AdcUtils {
  public static final String SEQUENCE_UMA_SCOPE = "sequence";
  public static final String REPERTOIRE_UMA_SCOPE = "repertoire";
  public static final String STATISTICS_UMA_SCOPE = "statistics";
  public static final String PUBLIC_ACCESS_LEVEL = "public"; // not actually an UMA scope
  public static final String UMA_STUDY_TYPE = "study";

  public static final List<String> AllUmaScopes = ImmutableList.of(SEQUENCE_UMA_SCOPE, REPERTOIRE_UMA_SCOPE, STATISTICS_UMA_SCOPE);

  public static final String REPERTOIRE_STUDY_ID_FIELD = "study_id";
  public static final String REPERTOIRE_STUDY_TITLE_FIELD = "study_title";
  public static final String REPERTOIRE_REPERTOIRE_ID_FIELD = "repertoire_id";
  public static final String REARRANGEMENT_REPERTOIRE_ID_FIELD = REPERTOIRE_REPERTOIRE_ID_FIELD;
  public static final String REARRANGEMENT_REARRANGEMENT_ID_FIELD = "rearrangement_id";

}
