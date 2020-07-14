package pt.inesctec.adcauthmiddleware.adc.jsonfilter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import pt.inesctec.adcauthmiddleware.utils.ThrowingConsumer;

public class AdcJsonWriter implements IAdcWriter {

  private final JsonGenerator generator;
  private boolean arrayMode = false;

  public AdcJsonWriter(OutputStream outputStream) throws IOException {
    this.generator =
        AdcJsonDocumentParser.JsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);

    this.generator.writeStartObject();
  }

  public void close() throws IOException {
    this.tryCloseArray();

    this.generator.writeEndObject();
    this.generator.flush();
    this.generator.close();
  }

  public void writeField(String field, ObjectNode value) throws IOException {
    this.tryCloseArray();

    this.generator.writeObjectField(field, value);
    this.generator.flush();
  }

  private void tryCloseArray() throws IOException {
    if (this.arrayMode) {
      this.generator.writeEndArray();
      this.arrayMode = false;
    }
  }

  public ThrowingConsumer<ObjectNode, IOException> buildArrayWriter(String fieldName)
      throws IOException {
    this.tryCloseArray();
    this.generator.writeArrayFieldStart(fieldName);
    this.arrayMode = true;

    return elem -> {
      this.generator.writeObject(elem);
      this.generator.flush();
    };
  }
}
