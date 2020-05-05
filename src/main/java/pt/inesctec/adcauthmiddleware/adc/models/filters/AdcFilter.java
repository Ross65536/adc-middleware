package pt.inesctec.adcauthmiddleware.adc.models.filters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.*;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "op",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PrimitiveContentFilter.class, name = "="),
    @JsonSubTypes.Type(value = PrimitiveContentFilter.class, name = "!="),
    @JsonSubTypes.Type(value = NumberContentFilter.class, name = "<"),
    @JsonSubTypes.Type(value = NumberContentFilter.class, name = "<="),
    @JsonSubTypes.Type(value = NumberContentFilter.class, name = ">"),
    @JsonSubTypes.Type(value = NumberContentFilter.class, name = ">="),
    @JsonSubTypes.Type(value = NoValueContentFilter.class, name = "is missing"),
    @JsonSubTypes.Type(value = NoValueContentFilter.class, name = "is"),
    @JsonSubTypes.Type(value = NoValueContentFilter.class, name = "is not missing"),
    @JsonSubTypes.Type(value = NoValueContentFilter.class, name = "not"),
    @JsonSubTypes.Type(value = PrimitiveListContentFilter.class, name = "in"),
    @JsonSubTypes.Type(value = PrimitiveListContentFilter.class, name = "exclude"),
    @JsonSubTypes.Type(value = StringContentFilter.class, name = "contains"),
    @JsonSubTypes.Type(value = LogicalFilter.class, name = "and"),
    @JsonSubTypes.Type(value = LogicalFilter.class, name = "or"),
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


