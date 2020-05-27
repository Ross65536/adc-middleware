package pt.inesctec.adcauthmiddleware.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static Map<String, Object> buildRepertoiresDocument(Map<String, Object> info, Object... repertoires) {
    return TestCollections.mapOf(
        Pair.of("Info", info), Pair.of("Repertoire", List.of(repertoires)));
  }

  public static Map<String, Object> buildRepertoiresDocumentWithInfo(Object... repertoires) {
    return buildRepertoiresDocument(buildInfo(), repertoires);
  }

  public static Object buildRearrangementsDocumentWithInfo(Object ... rearrangements) {
    return TestCollections.mapOf(
        Pair.of("Info", buildInfo()), Pair.of("Rearrangement", List.of(rearrangements)));
  }

  public static Object buildFacetsDocumentWithInfo(List<Map<String, Object>> facet) {
    return TestCollections.mapOf(
        Pair.of("Info", buildInfo()), Pair.of("Facet", facet));
  }

  public static Map<String, Object> buildAdcFields(String... fields) {
    return TestCollections.mapOf(Pair.of("fields", List.of(fields)));
  }

  public static Map<String, Object> buildAdcIncludeFields(String include) {
    return TestCollections.mapOf(Pair.of("include_fields", include));
  }

  public static Map<String, Object> buildAdcFields(Set<String> fields) {
    return TestCollections.mapOf(Pair.of("fields", fields));
  }

  public static Map<String, Object> buildAdcFacets(String field) {
    return TestCollections.mapOf(Pair.of("facets", field));
  }

  /**
   * Repertoire id and study id are set to param 'id'
   */
  public static Map<String, Object> buildRepertoire(String id) {
    var stringPrefix = id + "-";
    return TestCollections.mapOf(
        Pair.of(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD, "r" + id),
        Pair.of(
            AdcConstants.REPERTOIRE_STUDY_BASE, TestCollections.mapOf(
                Pair.of(AdcConstants.REPERTOIRE_STUDY_ID_BASE, "s" + id),
                Pair.of(AdcConstants.REPERTOIRE_STUDY_TITLE_BASE, stringPrefix + TestConstants.generateHexString(8)),
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

  public static Map<String, Object> buildRearrangement(String repertoireId, String rearrangementId) {
    var stringPrefix = rearrangementId + "-";
    return TestCollections.mapOf(
        Pair.of(AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD, repertoireId),
        Pair.of(AdcConstants.REARRANGEMENT_REARRANGEMENT_ID_FIELD, "r" + rearrangementId),
        Pair.of("sequence", stringPrefix + TestConstants.generateHexString(10)),
        Pair.of("sequence_id", stringPrefix + TestConstants.generateHexString(10)),
        Pair.of("sequence_aa", stringPrefix + TestConstants.generateHexString(10))
    );
  }

  public static List<Map<String, Object>> buildFacet(String field) {
    return List.of(
        Map.of(
            field, TestConstants.generateHexString(10),
            "count", TestConstants.Random.nextInt(20) + 1
        ),
        Map.of(
            field, TestConstants.generateHexString(10),
            "count", TestConstants.Random.nextInt(20) + 1
        ),
        Map.of(
            field, TestConstants.generateHexString(10),
            "count", TestConstants.Random.nextInt(20) + 1
        )
    );
  }

  public static Map<String, Object> buildUmaResource(String id, Collection<String> scopes) {
    var uniqueScopes = new HashSet<>(scopes);

    return TestCollections.mapOf(
        Pair.of("resource_id", id),
        Pair.of("resource_scopes", uniqueScopes)
    );
  }

  public static Map<String, Object> buildAdcFilters(String field) {
    return Map.of(
        "filters", Map.of(
            "op", "and",
            "content", List.of(
                Map.of(
                "op", "=",
                "content", Map.of(
                    "field", field,
                    "value", TestConstants.generateHexString(4)
                )),
                Map.of(
                    "op", "!=",
                    "content", Map.of(
                        "field", field,
                        "value", TestConstants.generateHexString(4)
                    ))
            )
      ));
  }

  public static Map<String, Object> buildAdcFacetsFilter(String field, List<String> values) {
    return Map.of(
        "filters", Map.of(
            "op", "in",
            "content", Map.of(
              "field", field,
              "value", values
                )));
  }
}