package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

public class StringContent extends PrimitiveValueContent {
  @Override
  protected void validateValue(Object value) {
    if (!(value instanceof String)) {
      throw new IllegalArgumentException("JSON Value must be a JSON string");
    }
  }
}
