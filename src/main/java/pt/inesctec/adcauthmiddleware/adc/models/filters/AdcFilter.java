package pt.inesctec.adcauthmiddleware.adc.models.filters;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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

    /**
     * Validate the user's request. Supposed to be overriden by extending filters.
     * Implementing classes are responsible for calling their corresponding
     * sub-filters or content objects's corresponding method.
     *
     * @param field           the path used for emitting user friendly error messages.
     * @param validFieldTypes the map of valid fields and their types.
     * @throws AdcException on validation error.
     */
    public void validate(String field, Map<String, FieldType> validFieldTypes) throws AdcException {
        FiltersUtils.assertNonNull(field + ".op", op);
    }

    /**
     * Add the fields from each operator to the parameter.
     * Implementing classes are responsible for calling their
     * corresponding sub-filters or content objects's corresponding method.
     *
     * @param fields the destination set for the fields.
     */
    @JsonIgnore
    public abstract void loadFields(Set<String> fields);

    /**
     * Add the operators in string form from each filter to the parameter.
     * Implementing classes are responsible for calling their corresponding
     * sub-filters or content objects's corresponding method.
     *
     * @param operators the destination set for the operators.
     */
    @JsonIgnore
    public void loadOperators(Set<String> operators) {
        operators.add(op);
    }
}
