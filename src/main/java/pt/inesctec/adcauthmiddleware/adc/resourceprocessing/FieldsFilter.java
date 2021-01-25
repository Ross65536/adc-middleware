package pt.inesctec.adcauthmiddleware.adc.resourceprocessing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.LoggerFactory;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

/**
 * Responsible for filtering out nested JSON fields from resources to which access was denied.
 * Field fragments as used here are the values between the fields separator.
 */
public class FieldsFilter implements IFieldsFilter {
    /**
     * The fields separator REGEX.
     */
    private static final String SEPARATOR = "\\.";
    private static org.slf4j.Logger Logger = LoggerFactory.getLogger(FieldsFilter.class);
    private final Function<String, Set<String>> fieldMapper;
    private final String idField;

    /**
     * constructor.
     *
     * @param fieldMapper maps an ADC resource's ID to the corresponding allowed fields for that specific resource. Fields not in the set will be removed.
     * @param idField     the name of the resource's ID field (key name).
     */
    public FieldsFilter(Function<String, Set<String>> fieldMapper, String idField) {
        this.fieldMapper = fieldMapper;
        this.idField = idField;
    }

    /**
     * Get a string field from a nested JSON objects given the complete field fragments.
     * Used for obtaining the resource's ID string.
     *
     * @param obj        JSON object
     * @param fieldParts field fragments.
     * @return the found string.
     */
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
            return FieldsFilter.getFieldRecursive(childObj, remainder);
        }

        if (!fieldNode.isTextual()) {
            return Optional.empty();
        }

        return Optional.of(fieldNode.textValue());
    }

    /**
     * Splits a set of fields on the fields separator ('.').
     * The first level fields (outermost fragments) are used as keys and inner field fragments are used as map values.
     *
     * @param fields fields to reduce
     * @return the map
     */
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

    /**
     * Removes fields from an JSON Object and call itself on fields that are nested JSON Objects.
     *
     * @param node   the JSON object.
     * @param fields the fields to permit, with separator but with the outer fragments removed.
     */
    private static void unsetObjectFieldsRecursive(ObjectNode node, Set<String> fields) {

        var reducedFields = FieldsFilter.reduceFieldsLevel(fields);

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
                FieldsFilter.unsetObjectFieldsRecursive(childObj, fieldRemainders);
                if (childObj.isEmpty()) {
                    node.remove(fieldName);
                }
            } else if (childNode.isArray()) {
                var childArr = (ArrayNode) childNode;

                for (int i = 0; i < childArr.size(); ) {
                    var elem = childArr.get(i);
                    if (!elem.isObject()) {
                        Logger.error(String.format("Invalid CSV config, expected %s.%s array element to be JSON object", fieldName, i));
                        childArr.remove(i);
                        continue;
                    }

                    var elemObj = (ObjectNode) elem;
                    FieldsFilter.unsetObjectFieldsRecursive(elemObj, fieldRemainders);
                    if (elemObj.isEmpty()) {
                        childArr.remove(i);
                        continue;
                    }

                    i++;
                }

                if (childArr.isEmpty()) {
                    node.remove(fieldName);
                }

            } else {
                Logger.error("Invalid CSV config, expected " + fieldName + " field to be JSON object or array");
                node.remove(fieldName);
            }
        }
    }

    /**
     * The filtering entrypoint.
     *
     * @param resource the resource to remove fields from. Modified by reference.
     * @return the filtered resource or empty. Empty can occur on an error or on an empty resource.
     */
    @Override
    public Optional<ObjectNode> mapResource(ObjectNode resource) {

        var names = ImmutableSet.copyOf(resource.fieldNames());
        if (names.size() == 0) {
            return Optional.empty();
        }

        var resourceId = FieldsFilter.getFieldRecursive(resource, this.idField.split(SEPARATOR));
        if (resourceId.isEmpty()) {
            Logger.error("Id field " + this.idField + " not present in returned JSON object");
            return Optional.empty();
        }

        var fields = this.fieldMapper.apply(resourceId.get());
        if (fields.isEmpty()) {
            return Optional.empty();
        }

        FieldsFilter.unsetObjectFieldsRecursive(resource, fields);

        if (resource.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(resource);
    }
}
