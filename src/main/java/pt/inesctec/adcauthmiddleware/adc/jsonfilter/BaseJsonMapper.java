package pt.inesctec.adcauthmiddleware.adc.jsonfilter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.http.Json;

public class BaseJsonMapper implements StreamingResponseBody {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(BaseJsonMapper.class);
  private static JsonFactory JsonFactory = new JsonFactory();
  static {
    JsonFactory.setCodec(Json.JsonObjectMapper);
  }

  private final IFieldsFilter filter;
  protected final InputStream response;
  protected final String mappedField;

  public BaseJsonMapper(InputStream response, String mappedField, IFieldsFilter filter) {
    this.filter = filter;
    this.response = response;
    this.mappedField = mappedField;
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    var parser = JsonFactory.createParser(response);
    var generator = JsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);

    generator.writeStartObject();

    if (parser.nextToken() != JsonToken.START_OBJECT) {
      Logger.error("Received response isn't a JSON object");
      generator.close();
      return;
    }

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      var fieldName = parser.getCurrentName();
      var nextToken = parser.nextToken();
      if (fieldName == null
          || (nextToken != JsonToken.START_OBJECT && nextToken != JsonToken.START_ARRAY)) {
        Logger.error("Malformed JSON received");
        generator.close();
        return;
      }

      if (fieldName.equals(AdcConstants.ADC_INFO)) {
        var map = parser.readValueAs(ObjectNode.class);
        generator.writeObjectField(AdcConstants.ADC_INFO, map);
      } else if (fieldName.equals(this.mappedField)) {
        processNode(parser, generator);
      } else {
        parser.skipChildren();
      }

      generator.flush();
    }

    generator.writeEndObject();
    generator.close();
  }

  private void processNode(JsonParser parser, JsonGenerator generator) throws IOException {
    generator.writeArrayFieldStart(this.mappedField);

    while (parser.nextToken() != JsonToken.END_ARRAY) {
      var map = parser.readValueAs(ObjectNode.class);
      var mapped = this.filter.mapResource(map);
      if (mapped.isPresent()) {
        generator.writeObject(mapped.get());
      }
    }

    generator.writeEndArray();
  }
}
