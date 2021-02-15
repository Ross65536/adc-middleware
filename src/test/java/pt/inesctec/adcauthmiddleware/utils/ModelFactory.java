package pt.inesctec.adcauthmiddleware.utils;

import pt.inesctec.adcauthmiddleware.adc.resources.RearrangementResource;
import pt.inesctec.adcauthmiddleware.adc.resources.RepertoireResource;

import java.util.*;
import java.util.stream.Collectors;

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

  private static Map<String, Object> buildRepertoiresDocument(
      Map<String, Object> info, Object... repertoires) {
    return TestCollections.mapOf(
        Pair.of("Info", info), Pair.of("Repertoire", List.of(repertoires)));
  }

  public static Map<String, Object> buildRepertoiresDocumentWithInfo(Object... repertoires) {
    return buildRepertoiresDocument(buildInfo(), repertoires);
  }

  public static Object buildRearrangementsDocumentWithInfo(Object... rearrangements) {
    return TestCollections.mapOf(
        Pair.of("Info", buildInfo()), Pair.of("Rearrangement", List.of(rearrangements)));
  }

  public static Object buildFacetsDocumentWithInfo(List<Map<String, Object>> facet) {
    return TestCollections.mapOf(Pair.of("Info", buildInfo()), Pair.of("Facet", facet));
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

  /** Repertoire id and study id are set to param 'id' */
  public static Map<String, Object> buildRepertoire(String id) {
    var stringPrefix = id + "-";
    return TestCollections.mapOf(
        Pair.of(RepertoireResource.ID_FIELD, "r" + id),
        Pair.of(
            RepertoireResource.STUDY_BASE,
            TestCollections.mapOf(
                Pair.of(RepertoireResource.STUDY_ID_BASE, "s" + id),
                Pair.of(
                    RepertoireResource.STUDY_TITLE_BASE,
                    stringPrefix + TestConstants.generateHexString(8)),
                Pair.of(
                    "study_type",
                    TestCollections.mapOf(
                        Pair.of("value", stringPrefix + TestConstants.generateHexString(2)),
                        Pair.of("inty", TestConstants.Random.nextInt()))))),
        Pair.of(
            "data_processing",
            TestCollections.mapOf(
                Pair.of(
                    "data_processing_files",
                    List.of(
                        stringPrefix + TestConstants.generateHexString(5),
                        stringPrefix + TestConstants.generateHexString(6))),
                Pair.of("numbo", TestConstants.Random.nextDouble()),
                Pair.of("boolo", TestConstants.Random.nextBoolean()))));
  }

  public static Map<String, Object> buildRearrangement(
      String repertoireId, String rearrangementId) {
    var stringPrefix = rearrangementId + "-";
    return TestCollections.mapOf(
        Pair.of(RearrangementResource.REPERTOIRE_ID_FIELD, repertoireId),
        Pair.of(RearrangementResource.ID_FIELD, "r" + rearrangementId),
        Pair.of("sequence", stringPrefix + TestConstants.generateHexString(10)),
        Pair.of("rearrangement_id", stringPrefix + TestConstants.generateHexString(10)),
        Pair.of("sequence_aa", stringPrefix + TestConstants.generateHexString(10)));
  }

  public static List<Map<String, Object>> buildFacets(String field) {
    return List.of(
        Map.of(
            field,
            TestConstants.generateHexString(10),
            "count",
            TestConstants.Random.nextInt(20) + 1),
        Map.of(
            field,
            TestConstants.generateHexString(10),
            "count",
            TestConstants.Random.nextInt(20) + 1),
        Map.of(
            field,
            TestConstants.generateHexString(10),
            "count",
            TestConstants.Random.nextInt(20) + 1));
  }

  public static Map<String, Object> buildFacet(String field, String value, int count) {
    return Map.of(
            field, value,
            "count", count);
  }

  public static List<Map<String, Object>> buildFacets(String field, Pair<String, Integer> ... facets) {
    return Arrays.stream(facets)
            .map(f -> buildFacet(field, f.first, f.second))
            .collect(Collectors.toList());
  }

  public static Map<String, Object> buildUmaResource(String id, Collection<String> scopes) {
    var uniqueScopes = new HashSet<>(scopes);

    return TestCollections.mapOf(
        Pair.of("resource_id", id), Pair.of("resource_scopes", uniqueScopes));
  }

  public static Map<String, Object> buildAdcFilters(Map<String, Object> filters) {
    return Map.of("filters", filters);
  }

  public static Map<String, Object> buildSimpleFilter(String op, String field, Object value) {
    return Map.of(
        "op", op,
        "content", Map.of(
                "field", field,
                    "value", value
            )
    );
  }

  public static Map<String, Object> buildComplexFilter(String field) {
    return Map.of(
        "op",
        "and",
        "content",
        List.of(
            Map.of(
                "op",
                "=",
                "content",
                Map.of("field", field, "value", TestConstants.generateHexString(4))),
            Map.of(
                "op",
                "!=",
                "content",
                Map.of("field", field, "value", TestConstants.generateHexString(4)))));
  }

  public static Map<String, Object> buildAdcFacetsFilter(String field, List<String> values) {
    return Map.of(
        "op",
        "in",
        "content",
        Map.of(
            "field", field,
            "value", values));
  }

  public static Map<String, Object> buildAndFilter(Map<String, Object>... filters) {
    return Map.of("op", "and", "content", List.of(filters));
  }

  public static Map<String, Object> buildTsvFormat() {
    return Map.of("format", "tsv");
  }
}
