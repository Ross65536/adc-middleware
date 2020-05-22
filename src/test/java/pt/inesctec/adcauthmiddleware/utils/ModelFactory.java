package pt.inesctec.adcauthmiddleware.utils;

import java.util.List;
import java.util.Map;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;

public class ModelFactory {

  public static Map<String, Object> buildInfo() {
    return TestCollections.mapOf(
        Pair.of("Title", "AIRR Data Commons API"),
        Pair.of("description", "API response for repertoire query"),
        Pair.of("version", 1.3),
        Pair.of(
            "contact",
            TestCollections.mapOf(
                Pair.of("name", "AIRR Community"),
                Pair.of("url", "https://github.com/airr-community"))));
  }

  public static Map<String, Object> buildRepertoiresDocument(Object info, Object... repertoires) {
    return TestCollections.mapOf(
        Pair.of("Info", info), Pair.of("Repertoire", List.of(repertoires)));
  }

  public static Map<String, Object> buildAdcSearch(String... fields) {
    return TestCollections.mapOf(Pair.of("fields", List.of(fields)));
  }

  /**
   * Repertoire id and study id are set to param 'id'
   */
  public static Map<String, Object> buildRepertoire(String id) {
    var stringPrefix = id + "-";
    return TestCollections.mapOf(
        Pair.of(AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD, id),
        Pair.of(
            "study", TestCollections.mapOf(
                Pair.of("study_id", id),
                Pair.of("study_title", stringPrefix + TestConstants.generateHexString(8)),
                Pair.of("study_type", TestCollections.mapOf(
                        Pair.of("value", stringPrefix + TestConstants.generateHexString(2)),
                        Pair.of("inty", TestConstants.Random.nextInt())))
        )),
        Pair.of("data_processing", TestCollections.mapOf(
            Pair.of("data_processing_files", List.of(
                stringPrefix +  TestConstants.generateHexString(5),
                stringPrefix + TestConstants.generateHexString(6)
            )),
            Pair.of("numbo", TestConstants.Random.nextDouble()),
            Pair.of("boolo", TestConstants.Random.nextBoolean())
        ))
    );
  }
}
