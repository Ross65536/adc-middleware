package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

public class PrimitiveValueContent extends FieldContent {
  private Object value;

  public Object getValue() {
    return value;
  }

  protected void validateValue(Object value) {
    assertPrimitiveType(value);
  }

  public void setValue(Object value) {
    this.validateValue(value);

    this.value = value;
  }
}
