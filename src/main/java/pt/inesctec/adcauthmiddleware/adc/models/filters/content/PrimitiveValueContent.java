package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import java.util.List;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

/**
 * Models a filter's content with an arbitrary value type, except arrays.
 */
public class PrimitiveValueContent extends FieldContent {
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) throws AdcException {
        this.validateValue(value);

        this.value = value;
    }

    protected void validateValue(Object value) throws AdcException {
        FiltersUtils.assertPrimitiveType("value", value);
    }

    @Override
    public void validate(String errorField, List<AdcFields> validFields) throws AdcException {
        super.validate(errorField, validFields);

        String fieldName = errorField + ".value";
        FiltersUtils.assertNonNull(fieldName, value);

        // TODO: Makes no sense to make these checks. Limits filtering possibilities way too much
        /*
        var fieldType = validFieldTypes.get(this.getField());

        switch (fieldType) {
            case NUMBER:
                FiltersUtils.assertNumber(fieldName, value);
                break;
            case BOOLEAN:
                if (!(value instanceof Boolean)) {
                    throw new AdcException("'" + fieldName + "' must be a JSON boolean");
                }
                break;
            case INTEGER:
                if (!(value instanceof Integer)) {
                    throw new AdcException("'" + fieldName + "' must be an integer");
                }
                break;
            case STRING:
                FiltersUtils.assertString(fieldName, value);
                break;
            case ARRAY_STRING:
                throw new AdcException(
                        String.format(
                                "'%s' cannot be a JSON array as indicated by field '%s'",
                                fieldName, this.getField()));
            default:
                throw new IllegalStateException("Unreachable");
        }
        */
    }
}
