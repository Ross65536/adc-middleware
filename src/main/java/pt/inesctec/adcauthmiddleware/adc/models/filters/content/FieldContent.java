package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

/**
 * Models a filter's content with only a field and no value.
 */
public class FieldContent {
    private String field;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    /**
     * Validate the content for semantic correctness.
     *
     * @param errorField      the fields path for better error messages.
     * @param validFieldTypes the valid fields and types.
     * @throws AdcException on validation error.
     */
    public void validate(String errorField, Map<String, FieldType> validFieldTypes)
            throws AdcException {
        String fieldName = errorField + ".field";
        FiltersUtils.assertNonNull(fieldName, field);
        if (!validFieldTypes.containsKey(field)) {
            throw new AdcException(String.format("'%s' value '%s' is not valid", fieldName, field));
        }
    }

    /**
     * Loads the content 'fields' value to the set parameter.
     *
     * @param fields destination set.
     */
    @JsonIgnore
    public void loadFields(Set<String> fields) {
        fields.add(field);
    }
}
