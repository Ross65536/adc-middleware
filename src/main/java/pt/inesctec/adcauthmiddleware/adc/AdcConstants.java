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
   * The repertoire's study ID field name (the parent resource's ID). The comma separator is hardcoded in other parts of code.
   */
  public static final String REPERTOIRE_STUDY_ID_FIELD = "study.study_id";
  /**
   * The repertoire's study title field name. The comma separator is hardcoded in other parts of code.
   */
  public static final String REPERTOIRE_STUDY_TITLE_FIELD = "study.study_title";
  /**
   * The repertoire's ID field name.
   */
  public static final String REPERTOIRE_REPERTOIRE_ID_FIELD = "repertoire_id";
  /**
   * The repertoire's study first fragment of the field. Used by JSON parser. In the code 2 levels are specified for the study ID field.
   */
  public static final String REPERTOIRE_STUDY_BASE = "study";
  /**
   * The repertoire's study's study ID second fragment name.
   */
  public static final String REPERTOIRE_STUDY_ID_BASE = "study_id";
  /**
   * The repertoire's study's study title second fragment name.
   */
  public static final String REPERTOIRE_STUDY_TITLE_BASE = "study_title";
  /**
   * The rearrangement's repertoire ID field name (the parent resource's ID).
   */
  public static final String REARRANGEMENT_REPERTOIRE_ID_FIELD = REPERTOIRE_REPERTOIRE_ID_FIELD;
  /**
   * The rearrangement's ID field name.
   */
  public static final String REARRANGEMENT_REARRANGEMENT_ID_FIELD = "sequence_id";

  /**
   * The set of all used and mandatory repertoire and rearrangement fields. Used for the CSV configuration file validation.
   */
  public static final Set<String> AllUsedFields =
      Set.of(
          REPERTOIRE_STUDY_ID_FIELD,
          REPERTOIRE_STUDY_TITLE_FIELD,
          REPERTOIRE_REPERTOIRE_ID_FIELD,
          REARRANGEMENT_REARRANGEMENT_ID_FIELD);

  /**
   * The ADC document (JSON object) response's field name for the repertoire list.
   */
  public static final String REPERTOIRE_RESPONSE_FILTER_FIELD = "Repertoire";
  /**
   * The ADC document (JSON object) response's field name for the rearrangement list.
   */
  public static final String REARRANGEMENT_RESPONSE_FILTER_FIELD = "Rearrangement";
  /**
   * The ADC document info parameter (passed to the user's response unmodified in some cases).
   */
  public static final String ADC_INFO = "Info";
  /**
   * The ADC document (JSON object) response's field name for a facets search's list of countings.
   */
  public static final String ADC_FACETS = "Facet";
  /**
   * The ADC field separator.
   */
  public static final String ADC_FIELD_SEPERATOR = ".";
}
