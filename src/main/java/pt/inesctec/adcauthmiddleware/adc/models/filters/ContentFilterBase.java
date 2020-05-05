package pt.inesctec.adcauthmiddleware.adc.models.filters;

import pt.inesctec.adcauthmiddleware.adc.models.filters.content.FieldContent;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.ValueContent;

public class ContentFilterBase<T extends FieldContent> extends AdcFilter {

  public class ValueContentFilter extends ContentFilterBase<ValueContent> {
  }

  private T content;

  public T getContent() {
    return content;
  }

  public void setContent(T content) {
    this.content = content;
  }
}
