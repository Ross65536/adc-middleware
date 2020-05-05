package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.FiltersUtils;

public class NumberContent extends PrimitiveValueContent {
  @Override
  protected void validateValue(Object value) throws AdcException {
    FiltersUtils.assertNumber("value", value);
  }
}
