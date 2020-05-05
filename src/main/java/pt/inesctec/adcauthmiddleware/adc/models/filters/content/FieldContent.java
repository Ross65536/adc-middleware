package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

public class FieldContent {
  protected String field;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  static void assertPrimitiveType(Object value) {
    if (!(value instanceof Integer || value instanceof Double || value instanceof Boolean || value instanceof String)) {
      throw new IllegalArgumentException("JSON Value must be a JSON primitive (number, boolean or string)");
    }
  }
}
