package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.Utils;
import pt.inesctec.adcauthmiddleware.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CsvConfig {

    private final Map<AdcField.Class, Map<AdcField.UmaScope, Map<String, AdcField>>> fieldsMapping;

    public CsvConfig(AppConfig config) throws Exception {
    var csvPath = config.getAdcCsvConfigPath();

    var fieldMappings = parseCsv(csvPath);
    Utils.jaxValidateList(fieldMappings);
    this.fieldsMapping = CollectionsUtils.buildMap(fieldMappings, AdcField::getFieldClass, AdcField::getUmaScope, AdcField::getField);
  }

  private List<AdcField> parseCsv(String csvPath) throws IOException {
    var file = new File(csvPath);

    var schema = CsvSchema.emptySchema().withHeader();
    return (List<AdcField>)
        (List<?>)
            new CsvMapper()
                .enable(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE)
                .enable(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS)
                .enable(CsvParser.Feature.TRIM_SPACES)
                .readerFor(AdcField.class)
                .with(schema)
                .readValues(file)
                .readAll();
  }

}
