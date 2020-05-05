package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

public class NumberContent extends PrimitiveValueContent {
  @Override
  protected void validateValue(Object value) {
    if (!(value instanceof Integer || value instanceof Double)) {
      throw new IllegalArgumentException("JSON Value must be a JSON number");
    }
  }
}
