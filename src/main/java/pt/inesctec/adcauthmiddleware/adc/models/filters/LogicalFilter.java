package pt.inesctec.adcauthmiddleware.adc.models.filters;

import java.util.List;

public class LogicalFilter extends AdcFilter {
  private List<AdcFilter> content;

  public List<AdcFilter> getContent() {
    return content;
  }

  public void setContent(List<AdcFilter> content) {
    this.content = content;
  }
}
