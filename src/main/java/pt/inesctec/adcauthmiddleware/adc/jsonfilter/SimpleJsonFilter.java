package pt.inesctec.adcauthmiddleware.adc.jsonfilter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.InputStream;

public class SimpleJsonFilter extends BaseJsonMapper {

  private final boolean filterOutField;

  public SimpleJsonFilter(InputStream response, String mappedField, boolean filterOutField) {
    super(response, mappedField);

    this.filterOutField = filterOutField;
  }

  @Override
  protected void guts(JsonParser parser, JsonGenerator generator) throws IOException {
    if (filterOutField) {
      generator.writeArrayFieldStart(this.mappedField);
      generator.writeEndArray();
      return;
    }

    generator.writeFieldName(this.mappedField);
    var tree = parser.readValueAsTree();
    generator.writeTree(tree);
  }
}
