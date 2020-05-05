package pt.inesctec.adcauthmiddleware.adc.models.filters.content;

import java.util.List;

public class PrimitiveListContent extends FieldContent {
  private List<Object> value;

  public List<Object> getValue() {
    return value;
  }

  public void setValue(List<Object> value) {
    if (! value.isEmpty()) {
      for (var val: value) {
        FieldContent.assertPrimitiveType(val);
      }
    }

    this.value = value;
  }
}
