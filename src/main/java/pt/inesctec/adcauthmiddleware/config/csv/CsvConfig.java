package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.Utils;
import pt.inesctec.adcauthmiddleware.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CsvConfig {

    private final Map<FieldClass, Map<AccessScope, Map<String, CsvField>>> fieldsMapping;

    public CsvConfig(AppConfig config) throws Exception {
    var csvPath = config.getAdcCsvConfigPath();

    var fieldMappings = parseCsv(csvPath);
    Utils.jaxValidateList(fieldMappings);
    this.fieldsMapping = CollectionsUtils.buildMap(fieldMappings, CsvField::getFieldClass, CsvField::getAccessScope, CsvField::getField);

    CollectionsUtils.assertListContains(this.fieldsMapping.keySet(), FieldClass.REPERTOIRE, FieldClass.REARRANGEMENT);
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

  private static Set<AccessScope> FilterScopes = ImmutableSet.of(AccessScope.PUBLIC);

  public Set<String> getUmaScopes(FieldClass fieldClass) {
    var scopes = this.fieldsMapping.get(fieldClass).keySet();
    return Sets.difference(scopes, FilterScopes)
        .stream()
        .map(Objects::toString)
        .collect(Collectors.toSet());


  }

}
