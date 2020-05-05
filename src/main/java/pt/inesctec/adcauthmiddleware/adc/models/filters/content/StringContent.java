package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;

public class StringContent extends PrimitiveValueContent {
  @Override
  protected void validateValue(Object value) throws AdcException {
    FiltersUtils.assertString("value", value);
  }
}
