package pt.inesctec.adcauthmiddleware.adc.jsonfilter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.ThrowingConsumer;

public class AdcTsvWriter implements IAdcWriter {

  public static class TsvBooleanSerializer extends JsonSerializer<Boolean> {
    @Override
    public void serialize(Boolean bool, JsonGenerator generator, SerializerProvider provider)
        throws IOException {
      generator.writeString(bool ? "T" : "F");
    }
  }

  private static final ObjectMapper Mapper = new ObjectMapper();

  private final OutputStream os;
  private Map<String, FieldType> headerFields;
  private ObjectWriter csvMapper;

  public AdcTsvWriter(OutputStream os, Map<String, FieldType> headerFields) {
    this.os = os;
    this.headerFields = headerFields;
  }

  @Override
  public void close() throws IOException {
    this.os.flush();
    this.os.close();
  }

  @Override
  public void writeField(String field, ObjectNode value) {}

  @Override
  public ThrowingConsumer<ObjectNode, IOException> buildArrayWriter(String fieldName) {
    return elem -> {
      var map = Mapper.convertValue(elem, new TypeReference<Map<String, Object>>() {});
      CollectionsUtils.stripNestedMaps(map);

      // workaround since jackson CSV doesn't seem to support streaming CSV writers
      if (this.csvMapper == null) {
        this.csvMapper = buildCsvMapper(this.headerFields, true);
        this.csvMapper.writeValue(this.os, map);
        this.csvMapper = buildCsvMapper(this.headerFields, false);
        return;
      }

      this.csvMapper.writeValue(this.os, map);
    };
  }

  private static ObjectWriter buildCsvMapper(
      Map<String, FieldType> headerFields, boolean writeHeaders) {
    CsvSchema.Builder schema = new CsvSchema.Builder();
    schema.setUseHeader(writeHeaders);
    schema.setColumnSeparator('\t');
    schema.setLineSeparator('\n');
    schema.setArrayElementSeparator(",");
    schema.disableQuoteChar();


    for (var p : headerFields.entrySet()) {
      var field = p.getKey();
      var type = p.getValue();

      switch (type) {
        case NUMBER:
        case INTEGER:
          schema.addNumberColumn(field);
          break;
        case STRING:
          schema.addColumn(field);
          break;
        case BOOLEAN:
          schema.addBooleanColumn(field);
          break;
        case ARRAY_STRING:
          schema.addArrayColumn(field);
          break;
        default:
          throw new IllegalArgumentException("Invalid");
      }
    }

    SimpleModule simpleModule = new SimpleModule("adc_tsv_boolean_serialization");
    simpleModule.addSerializer(Boolean.class, new TsvBooleanSerializer());
    return new CsvMapper()
        .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        .registerModule(simpleModule)
        .writerFor(Map.class)
        .with(schema.build());
  }
}
