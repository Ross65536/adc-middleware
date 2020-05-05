package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;

public class FieldContent {
  protected String field;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  static void assertPrimitiveType(String field, Object value) throws AdcException {
    if (!(value instanceof Integer || value instanceof Double || value instanceof Boolean || value instanceof String)) {
      throw new AdcException("'" + field + "' must be a JSON number, boolean or string");
    }
  }
}
