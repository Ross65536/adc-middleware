package pt.inesctec.adcauthmiddleware.adc.models.filters;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

import java.util.List;
import java.util.Map;

public class LogicalFilter extends AdcFilter {
  private List<AdcFilter> content;

  public List<AdcFilter> getContent() {
    return content;
  }

  public void setContent(List<AdcFilter> content) {
    this.content = content;
  }

  @Override
  public void validate(String field, Map<String, FieldType> validFieldTypes) throws AdcException {
    super.validate(field, validFieldTypes);
    FiltersUtils.assertNonNull(field + ".content", content);
    for (int i = 0; i < content.size(); i++) {
      content.get(i).validate(field + ".content." + i, validFieldTypes);
    }
  }
}
