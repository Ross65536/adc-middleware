package pt.inesctec.adcauthmiddleware.adc.resourceprocessing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.http.Json;

public class AdcJsonDocumentParser {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcJsonDocumentParser.class);
  public static JsonFactory JsonFactory = new JsonFactory();

  static {
    JsonFactory.setCodec(Json.JsonObjectMapper);
  }

  private final IFieldsFilter filter;
  protected final InputStream response;
  protected final String mappedField;
  private IAdcWriter adcWriter;

  private AdcJsonDocumentParser(InputStream response, String mappedField, IFieldsFilter filter, IAdcWriter adcWriter) {
    this.filter = filter;
    this.response = response;
    this.mappedField = mappedField;
    this.adcWriter = adcWriter;
  }

  private void process() throws IOException {
    var parser = JsonFactory.createParser(response);

    if (parser.nextToken() != JsonToken.START_OBJECT) {
      Logger.error("Received repository response isn't a JSON object");
      this.adcWriter.close();
      return;
    }

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      var fieldName = parser.getCurrentName();
      var nextToken = parser.nextToken();
      if (fieldName == null
          || (nextToken != JsonToken.START_OBJECT && nextToken != JsonToken.START_ARRAY)) {
        Logger.error("Malformed JSON received from repository");
        this.adcWriter.close();
        return;
      }

      if (fieldName.equals(AdcConstants.ADC_INFO)) {
        var map = parser.readValueAs(ObjectNode.class);
        this.adcWriter.writeField(AdcConstants.ADC_INFO, map);
      } else if (fieldName.equals(this.mappedField)) {
        processNode(parser);
      } else {
        parser.skipChildren();
      }
    }

    this.adcWriter.close();
  }

  private void processNode(JsonParser parser) throws IOException {
    var consumer = this.adcWriter.buildArrayWriter(this.mappedField);

    while (parser.nextToken() != JsonToken.END_ARRAY) {
      var map = parser.readValueAs(ObjectNode.class);
      var mapped = this.filter.mapResource(map);
      if (mapped.isPresent()) {
        consumer.accept(mapped.get());
      }
    }
  }

  public static StreamingResponseBody buildJsonMapper(InputStream response, String mappedField, IFieldsFilter filter) {
    return os -> {
      var jsonWriter = new AdcJsonWriter(os);
      var mapper = new AdcJsonDocumentParser(response, mappedField, filter, jsonWriter);
      mapper.process();
    };
  }

  public static StreamingResponseBody buildTsvMapper(InputStream response, String mappedField, IFieldsFilter filter, Map<String, FieldType> headerFields) {
    return os -> {

      var jsonWriter = new AdcTsvWriter(os, headerFields);
      var mapper = new AdcJsonDocumentParser(response, mappedField, filter, jsonWriter);
      mapper.process();
    };
  }
}
