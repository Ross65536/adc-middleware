package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;
import pt.inesctec.adcauthmiddleware.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CsvConfig {
  private static Set<AccessScope> FilterScopes = ImmutableSet.of(AccessScope.PUBLIC);

  private final Map<FieldClass, Map<AccessScope, Map<String, CsvField>>> fieldsMapping;

  public CsvConfig(AppConfig config) throws Exception {
    var csvPath = config.getAdcCsvConfigPath();

    var fieldMappings = parseCsv(csvPath);
    Utils.jaxValidateList(fieldMappings);
    this.fieldsMapping =
        CollectionsUtils.buildMap(
            fieldMappings, CsvField::getFieldClass, CsvField::getAccessScope, CsvField::getField);

    CollectionsUtils.assertListContains(
        this.fieldsMapping.keySet(), FieldClass.REPERTOIRE, FieldClass.REARRANGEMENT);
  }

  public Set<String> getUmaScopes(FieldClass fieldClass) {
    var scopes = this.fieldsMapping.get(fieldClass).keySet();
    return Sets.difference(scopes, FilterScopes).stream()
        .map(Objects::toString)
        .collect(Collectors.toSet());
  }

  public Set<String> getUmaScopes(FieldClass fieldClass, Collection<String> fieldsFilter) {
    var fields = new HashSet<>(fieldsFilter);
    var classScopes = this.fieldsMapping.get(fieldClass);
    var scopes = classScopes.keySet();

    return Sets.difference(scopes, FilterScopes).stream()
        .filter(scope -> {
          Set<String> actualFields = classScopes.get(scope).keySet();
          var diff = Sets.difference(actualFields, fields);
          Sets.SetView<String> intersection = Sets.intersection(actualFields, fields);
          return !intersection
                .isEmpty();
                }
        ).map(Objects::toString)
        .collect(Collectors.toSet());
  }

  public Set<String> getFields(FieldClass fieldClass) {
    return this.fieldsMapping.get(fieldClass).values().stream()
        .map(Map::keySet)
        .reduce(new HashSet<>(), Sets::union);
  }

  public Set<String> getFields(FieldClass fieldClass, Set<AccessScope> scopes) {
    var classFields = this.fieldsMapping.get(fieldClass);

    var set =
        scopes.stream()
            .filter(classFields::containsKey)
            .map(scope -> classFields.get(scope).keySet())
            .reduce(new HashSet<>(), Sets::union);

    return set;
  }

  private List<CsvField> parseCsv(String csvPath) throws IOException {
    var file = new File(csvPath);

    var schema = CsvSchema.emptySchema().withHeader();
    return (List<CsvField>)
        (List<?>)
            new CsvMapper()
                .enable(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE)
                .enable(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS)
                .enable(CsvParser.Feature.TRIM_SPACES)
                .readerFor(CsvField.class)
                .with(schema)
                .readValues(file)
                .readAll();
  }
}
