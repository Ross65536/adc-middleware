package pt.inesctec.adcauthmiddleware.adc.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "op",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LogicalFilter.class, name = "or"),
    @JsonSubTypes.Type(value = LogicalFilter.class, name = "and"),
    @JsonSubTypes.Type(value = ValueContentFilter.class, name = "="),
})
public abstract class AdcFilter {
  protected String op;

  public String getOp() {
    return this.op;
  }

  public void setOp(String op) {
    this.op = op;
  }
}
