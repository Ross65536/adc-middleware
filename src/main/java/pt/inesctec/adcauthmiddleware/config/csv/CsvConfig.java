package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CsvConfig {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(CsvConfig.class);
  private static Set<AccessScope> FilterScopes = ImmutableSet.of(AccessScope.PUBLIC);

  private final Map<FieldClass, Map<AccessScope, Map<String, CsvField>>> fieldsMapping;

  public CsvConfig(AppConfig config) throws Exception {
    var csvPath = config.getAdcCsvConfigPath();
    var file = loadCsvFile(csvPath);


    var fieldMappings = parseCsv(file);
    validateCsvFields(fieldMappings);
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

  public Map<String, FieldType> getFields(FieldClass fieldClass) {
    Map<AccessScope, Map<String, CsvField>> classScopes = this.fieldsMapping.get(fieldClass);
    return this.fieldsMapping.get(fieldClass)
        .values()
        .stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(CsvField::getField, CsvField::getFieldType));
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

  private List<CsvField> parseCsv(File file) throws IOException {

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

  private File loadCsvFile(String userCsvPath) {
    if (userCsvPath == null) {
      Logger.info("Loading default field mapping csv file from resources folder");

      URL resource = this.getClass().getClassLoader().getResource("field-mapping.csv");
      if (resource == null) {
        throw new IllegalStateException("Invalid resources configuration, missing field-mapping.csv file");
      }
      return new File(resource.getFile());
    }

    Logger.info("Loading field mapping csv file from path: " + userCsvPath);
    var file = new File(userCsvPath);

    if (! file.exists()) {
      Logger.error("Field mapping csv file doesn't exist: " + userCsvPath);
      throw new IllegalArgumentException(userCsvPath + " doesn't exist");
    }

    if (file.isDirectory()) {
      Logger.error("Field mapping csv file must be file: " + userCsvPath);
      throw new IllegalArgumentException(userCsvPath + " is not file");

    }

    return file;
  }

  private static void validateCsvFields(List<CsvField> fields) throws Exception {
    Utils.jaxValidateList(fields);
    Map<FieldClass, Set<String>> uniqueFields = new HashMap<>();
    uniqueFields.put(FieldClass.REARRANGEMENT, new HashSet<>());
    uniqueFields.put(FieldClass.REPERTOIRE, new HashSet<>());

    for (var field: fields) {
      var fieldClass = field.getFieldClass();
      var fieldPath = field.getField();

      Set<String> set = uniqueFields.get(fieldClass);
      if (set.contains(fieldPath)) {
        Logger.error("csv field mapping config file must not have duplicate field values for the same class");
        throw new IllegalArgumentException("csv field mapping config file must not have duplicate field values for the same class");
      }

      set.add(fieldPath);
    }

  }
}
