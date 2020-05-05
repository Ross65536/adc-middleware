package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;

public class NumberContent extends PrimitiveValueContent {
  @Override
  protected void validateValue(Object value) throws AdcException {
    if (!(value instanceof Integer || value instanceof Double)) {
      throw new AdcException("'value' must be a JSON number");
    }
  }
}
