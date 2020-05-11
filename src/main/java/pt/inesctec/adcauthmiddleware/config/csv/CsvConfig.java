package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

@Component
public class CsvConfig {
  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(CsvConfig.class);
  private static final Set<String> FilterPublicScopes = CollectionsUtils.immutableSetWithNull();

  private final Map<FieldClass, Map<String, Map<String, CsvField>>> fieldsMapping;

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

  public Set<String> getAllUmaScopes() {
    return this.fieldsMapping.values().stream()
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  public Set<String> getUmaScopes(FieldClass fieldClass) {

    return this.fieldsMapping.get(fieldClass).keySet().stream()
        .filter(Objects::nonNull) // filter out public
        .collect(Collectors.toSet());
  }

  public Set<String> getUmaScopes(FieldClass fieldClass, Collection<String> fieldsFilter) {
    var fields = new HashSet<>(fieldsFilter);
    var classScopes = this.fieldsMapping.get(fieldClass);

    return this.fieldsMapping.get(fieldClass).keySet().stream()
        .filter(Objects::nonNull) // filter out public
        .filter(scope -> !Sets.intersection(classScopes.get(scope).keySet(), fields).isEmpty())
        .map(Objects::toString)
        .collect(Collectors.toSet());
  }

  public Set<String> getPublicFields(FieldClass fieldClass) {
    return this.getFields(fieldClass, FilterPublicScopes);
  }

  public Map<String, FieldType> getFields(FieldClass fieldClass) {
    return this.fieldsMapping.get(fieldClass).values().stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(CsvField::getField, CsvField::getFieldType));
  }

  /**
   * Get fields for class and scopes.
   *
   * @param fieldClass the class
   * @param scopes a null scope element value will return public fields
   * @return the fields
   */
  public Set<String> getFields(FieldClass fieldClass, Set<String> scopes) {
    var classFields = this.fieldsMapping.get(fieldClass);

    return scopes.stream()
        .filter(classFields::containsKey)
        .map(scope -> classFields.get(scope).keySet())
        .reduce(new HashSet<>(), Sets::union);
  }

  private static void validateCsvFields(List<CsvField> fields) throws Exception {
    Utils.jaxValidateList(fields);
    Map<FieldClass, Set<String>> uniqueFields = new HashMap<>();
    uniqueFields.put(FieldClass.REARRANGEMENT, new HashSet<>());
    uniqueFields.put(FieldClass.REPERTOIRE, new HashSet<>());

    for (var field : fields) {
      var fieldClass = field.getFieldClass();
      var fieldPath = field.getField();
      String errorMsg = String.format("csv field mapping config: row %s:%s", fieldClass, fieldPath);

      if (!field.isPublic() && field.isEmptyScope()) {
        String msg = errorMsg + " can't be protected and have no access scope";
        Logger.error(msg);
        throw new IllegalArgumentException(msg);
      } else if (field.isPublic() && !field.isEmptyScope()) {
        String msg = errorMsg + " can't be public and have a access scope";
        Logger.error(msg);
        throw new IllegalArgumentException(msg);
      }

      Set<String> set = uniqueFields.get(fieldClass);
      if (set.contains(fieldPath)) {
        String msg = errorMsg + " must not be duplicate by field for the same class " + fieldClass;
        Logger.error(msg);
        throw new IllegalArgumentException(msg);
      }

      set.add(fieldPath);
    }

    var requiredFieldsPresent =
        uniqueFields.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet())
            .containsAll(AdcConstants.AllUsedFields);
    if (!requiredFieldsPresent) {
      String msg =
          "All required fields: "
              + CollectionsUtils.toString(AdcConstants.AllUsedFields)
              + " must be present in field mappings csv";
      Logger.error(msg);
      throw new IllegalArgumentException(msg);
    }
  }

  private InputStream loadCsvFile(String userCsvPath) {
    if (userCsvPath == null) {
      Logger.info("Loading default field mapping csv file from resources folder");

      var resource = this.getClass().getClassLoader().getResourceAsStream("field-mapping.csv");
      if (resource == null) {
        throw new IllegalStateException(
            "Invalid resources configuration, missing field-mapping.csv file");
      }
      return resource;
    }

    Logger.info("Loading field mapping csv file from path: " + userCsvPath);
    var file = new File(userCsvPath);

    if (file.isDirectory()) {
      Logger.error("Field mapping csv file must be file: " + userCsvPath);
      throw new IllegalArgumentException(userCsvPath + " is not file");
    }

    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      Logger.error("Field mapping csv file doesn't exist: " + userCsvPath);
      throw new IllegalArgumentException(userCsvPath + " doesn't exist", e);
    }
  }

  private static List<CsvField> parseCsv(InputStream file) throws IOException {

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
