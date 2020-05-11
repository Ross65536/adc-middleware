package pt.inesctec.adcauthmiddleware.adc.models.filters;

import java.util.Map;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.FieldContent;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

public class ContentFilterBase<T extends FieldContent> extends AdcFilter {
  private T content;

  public T getContent() {
    return content;
  }

  public void setContent(T content) {
    this.content = content;
  }

  @Override
  public void validate(String field, Map<String, FieldType> validFieldTypes) throws AdcException {
    super.validate(field, validFieldTypes);
    FiltersUtils.assertNonNull(field + ".content", content);
    content.validate(field + ".content", validFieldTypes);
    //    for
  }
}
