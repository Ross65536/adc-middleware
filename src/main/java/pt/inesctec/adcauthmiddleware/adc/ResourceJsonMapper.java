package pt.inesctec.adcauthmiddleware.adc;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNullApi;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ResourceJsonMapper implements StreamingResponseBody {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceJsonMapper.class);
  private static JsonFactory JsonFactory = new JsonFactory();
  static {
    JsonFactory.setCodec(HttpFacade.JsonObjectMapper);
  }

  private final InputStream response;
  private final String mappedField;
  private final Function<String, Set<String>> fieldMapper;
  private final String idField;

  public ResourceJsonMapper(InputStream response, String mappedField, Function<String, Set<String>> fieldMapper, String idField) {

    this.response = response;
    this.mappedField = mappedField;
    this.fieldMapper = fieldMapper;
    this.idField = idField;
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
      if (fieldName == null || (nextToken != JsonToken.START_OBJECT && nextToken != JsonToken.START_ARRAY)) {
        Logger.error("Malformed JSON received");
        generator.close();
        return;
      }

      if (fieldName.equals("Info")) {
        var map = parser.readValueAs(ObjectNode.class);
        generator.writeObjectField("Info", map);
      } else if (fieldName.equals(this.mappedField)) {
        generator.writeArrayFieldStart(this.mappedField);
        while (parser.nextToken() != JsonToken.END_ARRAY) {
          var map = parser.readValueAs(ObjectNode.class);
          var mapped = this.mapResource(map);
          if (mapped.isPresent()) {
            generator.writeObject(mapped.get());
          }
        }
        generator.writeEndArray();
      } else {
        parser.skipChildren();
      }

      generator.flush();
    }

    generator.writeEndObject();
    generator.close();
  }

  private Optional<ObjectNode> mapResource(ObjectNode resource) {

    var names = ImmutableSet.copyOf(resource.fieldNames());
    if (names.size() == 0) {
      return Optional.empty();
    }



//    var retObj = new ObjectNode(JsonNodeFactory.instance);
    return Optional.of(resource);
  }


}
