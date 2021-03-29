package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import java.util.List;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

/**
 * Models a filter's content with a value of array os strings type.
 */
public class PrimitiveListContent extends FieldContent {
    private List<Object> value;

    public List<Object> getValue() {
        return value;
    }

    public void setValue(List<Object> value) throws AdcException {
        if (!value.isEmpty()) {
            for (int i = 0; i < value.size(); i++) {
                var val = value.get(i);
                FiltersUtils.assertPrimitiveType("value." + i, val);
            }
        }

        this.value = value;
    }

    @Override
    public void validate(String errorField, List<AdcFields> validFields) throws AdcException {
        super.validate(errorField, validFields);

        String fieldName = errorField + ".value";
        FiltersUtils.assertNonNull(fieldName, value);

        // TODO: Disabled for the same reasons specified in PrimitiveValueContent
        /*
        var fieldType = validFields.get(this.getField());

        switch (fieldType) {
            case ARRAY_STRING:
                for (int i = 0; i < value.size(); i++) {
                    var val = value.get(i);
                    var subName = fieldName + "." + i;
                    FiltersUtils.assertString(subName, val);
                }
                break;
            default:
                throw new AdcException(String.format(
                    "'%s' must be of type JSON string array as indicated by field '%s'", fieldName, this.getField()
                ));
        }
        */
    }
}
