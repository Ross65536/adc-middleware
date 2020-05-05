package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;

import java.util.List;

public class PrimitiveListContent extends FieldContent {
  private List<Object> value;

  public List<Object> getValue() {
    return value;
  }

  public void setValue(List<Object> value) throws AdcException {
    if (! value.isEmpty()) {
      for (int i = 0; i < value.size(); i++) {
        var val = value.get(i);
        FieldContent.assertPrimitiveType("value." + i, val);
      }
    }

    this.value = value;
  }
}
