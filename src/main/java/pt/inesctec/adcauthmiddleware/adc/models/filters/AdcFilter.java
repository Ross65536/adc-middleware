package pt.inesctec.adcauthmiddleware.adc.models.filters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.NoValueContentFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.NumberContentFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.PrimitiveContentFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.PrimitiveListContentFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.StringContentFilter;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "op",
    visible = true)
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

  public void validate(String field, Map<String, FieldType> validFieldTypes) throws AdcException {
    FiltersUtils.assertNonNull(field + ".op", op);
  }
}
