package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

import java.util.Map;

/**
 * Models a filter's content with an arbitrary value type, except arrays.
 */
public class PrimitiveValueContent extends FieldContent {
  private Object value;

  public Object getValue() {
    return value;
  }

  protected void validateValue(Object value) throws AdcException {
    FiltersUtils.assertPrimitiveType("value", value);
  }

  public void setValue(Object value) throws AdcException {
    this.validateValue(value);

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
  }
}
