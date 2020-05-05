package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;

public class PrimitiveValueContent extends FieldContent {
  private Object value;

  public Object getValue() {
    return value;
  }

  protected void validateValue(Object value) throws AdcException {
    assertPrimitiveType("value", value);
  }

  public void setValue(Object value) throws AdcException {
    this.validateValue(value);

    this.value = value;
  }
}
