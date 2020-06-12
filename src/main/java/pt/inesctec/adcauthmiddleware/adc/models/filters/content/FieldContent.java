package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import java.util.Map;
import java.util.Set;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

public class FieldContent {
  private String field;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public void validate(String errorField, Map<String, FieldType> validFieldTypes)
      throws AdcException {
    String fieldName = errorField + ".field";
    FiltersUtils.assertNonNull(fieldName, field);
    if (!validFieldTypes.containsKey(field)) {
      throw new AdcException(String.format("'%s' value '%s' is not valid", fieldName, field));
    }
  }

  public void loadFields(Set<String> fields) {
    fields.add(field);
  }
}
