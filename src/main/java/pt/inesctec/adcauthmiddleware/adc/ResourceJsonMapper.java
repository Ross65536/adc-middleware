package pt.inesctec.adcauthmiddleware.adc;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.http.Json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;

public class ResourceJsonMapper implements StreamingResponseBody {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceJsonMapper.class);
  private static JsonFactory JsonFactory = new JsonFactory();
  static {
    JsonFactory.setCodec(Json.JsonObjectMapper);
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

  private static final String SEPARATOR = "\\.";

  private static Optional<String> getFieldRecursive(ObjectNode obj, String[] fieldParts) {
    if (fieldParts.length == 0) {
      return Optional.empty();
    }

    var fieldName = fieldParts[0];
    var remainder = CollectionsUtils.popFront(fieldParts);
    if (!obj.has(fieldName)) {
      return Optional.empty();
    }

    var fieldNode = obj.get(fieldName);

    if (remainder.length != 0) {
      if (! fieldNode.isObject()) {
        return Optional.empty();
      }

      var childObj = (ObjectNode) fieldNode;
      return ResourceJsonMapper.getFieldRecursive(childObj, remainder);
    }

    if (! fieldNode.isTextual()) {
      return Optional.empty();
    }

    return Optional.of(fieldNode.textValue());
  }

  private static Map<String, Set<String>> reduceFieldsLevel(Set<String> fields) {
    var map = new HashMap<String, Set<String>>();

    for (var field : fields) {
      var parts = field.split(SEPARATOR, 2);
      var first = parts[0];

      if (! map.containsKey(first)) {
        map.put(first, new HashSet<>());
      }

      if (parts.length == 2) {
        String remainder = parts[1];
        map.get(first)
            .add(remainder);
      }
    }

    return map;
  }


  private static void unsetObjectFieldsRecursive(ObjectNode node, Set<String> fields) {

    var reducedFields = ResourceJsonMapper.reduceFieldsLevel(fields);

    for (String fieldName : ImmutableList.copyOf(node.fieldNames())) {
      if (! reducedFields.containsKey(fieldName)) {
        node.remove(fieldName);
        continue;
      }

      var fieldRemainders = reducedFields.get(fieldName);
      if (fieldRemainders.isEmpty()) {
        continue;
      }

      var childNode = node.get(fieldName);
      if (! childNode.isObject()) {
        Logger.error("Invalid CSV config, expected " + fieldName + " field to be object");
        continue;
      }

      var childObj = (ObjectNode) childNode;
      ResourceJsonMapper.unsetObjectFieldsRecursive(childObj, fieldRemainders);
      if (childObj.isEmpty()) {
        node.remove(fieldName);
      }
    }
  }

  private Optional<ObjectNode> mapResource(ObjectNode resource) {

    var names = ImmutableSet.copyOf(resource.fieldNames());
    if (names.size() == 0) {
      return Optional.empty();
    }

    var resourceId = ResourceJsonMapper.getFieldRecursive(resource, this.idField.split(SEPARATOR));
    if (resourceId.isEmpty()) {
      Logger.error("Id field " + this.idField + " not present in returned JSON object");
      return Optional.empty();
    }

    var fields = this.fieldMapper.apply(resourceId.get());
    if (fields.isEmpty()) {
      return Optional.empty();
    }

    ResourceJsonMapper.unsetObjectFieldsRecursive(resource, fields);

    return Optional.of(resource);
  }


}
