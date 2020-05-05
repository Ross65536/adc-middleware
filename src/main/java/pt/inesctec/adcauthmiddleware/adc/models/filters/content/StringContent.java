package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;

public class StringContent extends PrimitiveValueContent {
  @Override
  protected void validateValue(Object value) throws AdcException {
    if (!(value instanceof String)) {
      throw new AdcException("'value' must be a JSON string");
    }
  }
}
