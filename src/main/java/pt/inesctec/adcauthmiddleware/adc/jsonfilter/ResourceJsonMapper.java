package pt.inesctec.adcauthmiddleware.adc.jsonfilter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.LoggerFactory;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

public class ResourceJsonMapper extends BaseJsonMapper {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceJsonMapper.class);

  private final Function<String, Set<String>> fieldMapper;
  private final String idField;

  public ResourceJsonMapper(
      InputStream response,
      String mappedField,
      Function<String, Set<String>> fieldMapper,
      String idField) {
    super(response, mappedField);
    this.fieldMapper = fieldMapper;
    this.idField = idField;
  }

  @Override
  protected void guts(JsonParser parser, JsonGenerator generator) throws IOException {
    generator.writeArrayFieldStart(this.mappedField);

    while (parser.nextToken() != JsonToken.END_ARRAY) {
      var map = parser.readValueAs(ObjectNode.class);
      var mapped = this.mapResource(map);
      if (mapped.isPresent()) {
        generator.writeObject(mapped.get());
      }
    }

    generator.writeEndArray();
  }

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
      if (!fieldNode.isObject()) {
        return Optional.empty();
      }

      var childObj = (ObjectNode) fieldNode;
      return ResourceJsonMapper.getFieldRecursive(childObj, remainder);
    }

    if (!fieldNode.isTextual()) {
      return Optional.empty();
    }

    return Optional.of(fieldNode.textValue());
  }

  private static final String SEPARATOR = "\\.";

  private static Map<String, Set<String>> reduceFieldsLevel(Set<String> fields) {
    var map = new HashMap<String, Set<String>>();

    for (var field : fields) {
      var parts = field.split(SEPARATOR, 2);
      var first = parts[0];

      if (!map.containsKey(first)) {
        map.put(first, new HashSet<>());
      }

      if (parts.length == 2) {
        String remainder = parts[1];
        map.get(first).add(remainder);
      }
    }

    return map;
  }

  private static void unsetObjectFieldsRecursive(ObjectNode node, Set<String> fields) {

    var reducedFields = ResourceJsonMapper.reduceFieldsLevel(fields);

    for (String fieldName : ImmutableList.copyOf(node.fieldNames())) {
      if (!reducedFields.containsKey(fieldName)) {
        node.remove(fieldName);
        continue;
      }

      var fieldRemainders = reducedFields.get(fieldName);
      if (fieldRemainders.isEmpty()) {
        continue;
      }

      var childNode = node.get(fieldName);
      if (childNode.isObject()) {
        var childObj = (ObjectNode) childNode;
        ResourceJsonMapper.unsetObjectFieldsRecursive(childObj, fieldRemainders);
        if (childObj.isEmpty()) {
          node.remove(fieldName);
        }
      } else if (childNode.isArray()) {
        var childArr = (ArrayNode) childNode;

        for (int i = 0; i < childArr.size();) {
          var elem = childArr.get(i);
          if (! elem.isObject()) {
            Logger.error(String.format("Invalid CSV config, expected %s.%s array element to be JSON object", fieldName, i));
            childArr.remove(i);
            continue;
          }

          var elemObj = (ObjectNode) elem;
          ResourceJsonMapper.unsetObjectFieldsRecursive(elemObj, fieldRemainders);
          if (elemObj.isEmpty()) {
            childArr.remove(i);
            continue;
          }

          i++;
        }
      } else  {
        Logger.error("Invalid CSV config, expected " + fieldName + " field to be JSON object or array");
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

    if (resource.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(resource);
  }
}
