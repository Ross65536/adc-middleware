package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import java.util.List;
import java.util.Map;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

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
  public void validate(String errorField, Map<String, FieldType> validFieldTypes)
      throws AdcException {
    super.validate(errorField, validFieldTypes);

    String fieldName = errorField + ".value";
    FiltersUtils.assertNonNull(fieldName, value);

    var fieldType = validFieldTypes.get(this.getField());
    switch (fieldType) {
      case ARRAY_STRING:
        for (int i = 0; i < value.size(); i++) {
          var val = value.get(i);
          var subName = fieldName + "." + i;
          FiltersUtils.assertString(subName, val);
        }
        break;
      default:
        throw new AdcException(
            String.format(
                "'%s' must be of type JSON string array as indicated by field '%s'",
                fieldName, this.getField()));
    }
  }
}
