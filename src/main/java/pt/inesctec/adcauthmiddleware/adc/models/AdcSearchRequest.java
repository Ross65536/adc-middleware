package pt.inesctec.adcauthmiddleware.adc.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.filters.AdcFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.LogicalFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.PrimitiveListContent;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.PrimitiveListContentFilter;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.IncludeField;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

/**
 * Models a user's ADC search request body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdcSearchRequest {
    private static final Set<String> SupportedFormats = Set.of("json", "tsv");
    private AdcFilter filters;
    private Set<String> fields;
    private Long from;
    private Long size;
    private String format;
    private String facets;
    @JsonProperty("include_fields")
    private IncludeField includeFields;

    public AdcSearchRequest() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    /**
     * Validates that the user request is semantically correct.
     *
     * @param validFields   list of all valid fields for the resource type of the endpoint and their corresponding types.
     * @param tsvRequestedFields the set of fields that are requested (from 'fields', 'include_fields'). Only applicable in a TSV request.
     * @throws AdcException on validation error.
     */
    @JsonIgnore
    public void validate(List<AdcFields> validFields, Set<String> tsvRequestedFields) throws AdcException {
        var fields = this.getFields();
        var validFieldNames = AdcFields.toNameList(validFields);

        if (fields != null && this.getFacets() != null) {
            throw new AdcException("Can't use 'fields' and 'facets' at the same time in request");
        }

        if (this.getIncludeFields() != null && this.getFacets() != null) {
            throw new AdcException("Can't use 'include_fields' and 'facets' at the same time in request");
        }

        // Validate if unknown fields are being requested
        if (fields != null && !fields.isEmpty()) {
            var fieldsToValidate = new ArrayList<>(fields);
            fieldsToValidate.removeAll(validFieldNames);

            if (!fieldsToValidate.isEmpty()) {
                throw new AdcException(
                    String.format("One or more fields aren't known ADC Fields: %s", fields.toString())
                );
            }
        }

        if (this.facets != null && !validFieldNames.contains(this.facets)) {
            throw new AdcException(String.format("'facets' '%s' value is not valid", this.facets));
        }

        if (this.filters != null) {
            this.filters.validate("filters", validFields);
        }

        if (this.isTsvFormat()) {
            if (this.isFacetsSearch()) {
                throw new AdcException("can't return TSV format for facets");
            }

            for (var field : tsvRequestedFields) {
                if (field.contains(AdcConstants.ADC_FIELD_SEPARATOR)) {
                    throw new AdcException(
                        String.format("TSV: The field %s requested cannot be a nested document", field)
                    );
                }
            }
        }
    }

    /**
     * Get the fields that correspond to this Request, non-facets.
     * This includes the "fields" and "include_fields" parameters.
     * If both empty all of the resource's fields are returned.
     *
     * @param fieldClass the resource type
     * @param csvConfig CsvConfig object
     * @return the set of fields that were requested.
     */
    @JsonIgnore
    public Set<String> getRequestedFieldsCsv(FieldClass fieldClass, CsvConfig csvConfig) {
        final Set<String> fields = this.isFieldsEmpty() ? Set.of() : this.getFields();

        final Set<String> includeFields = this.isIncludeFieldsEmpty()
            ? Set.of()
            : csvConfig.getFields(fieldClass, this.getIncludeFields());

        final Set<String> requestedFields = Sets.union(fields, includeFields);

        return new HashSet<>(requestedFields.isEmpty()
                ? csvConfig.getFieldsTypes(fieldClass).keySet()
                : requestedFields);
    }

    /**
     * Get the fields that correspond to this Request, non-facets.
     * This includes the "fields" attribute and specific fields present in "filter" operations.
     * If no specific fields were requested, an empty Set will be returned, meaning the user
     * requested no fields.
     *
     * @return the set requested fields.
     */
    // TODO: Could expand this method to check "include_fields" somehow?
    @JsonIgnore
    public Set<String> getRequestedFields() {
        final Set<String> fields       = this.isFieldsEmpty() ? Set.of() : this.getFields();
        final Set<String> filterFields = this.getRequestedFilterFields();

        /*final Set<String> includeFields = this.isIncludeFieldsEmpty()
            ? Set.of()
            : csvConfig.getFields(fieldClass, this.getIncludeFields());*/

        return Sets.union(fields, filterFields);
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) throws AdcException {
        if (!SupportedFormats.contains(format)) {
            throw new AdcException(
                    String.format("'format' value '%s' not supported, use 'json' or 'tsv'", format));
        }

        this.format = format;
    }

    public String getFacets() {
        return facets;
    }

    public void setFacets(String facets) {
        this.facets = facets;
    }

    @JsonIgnore
    public boolean isJsonFormat() {
        return format == null || format.equals("json");
    }

    @JsonIgnore
    public boolean isTsvFormat() {
        return format != null && format.equals("tsv");
    }

    @JsonIgnore
    public boolean isFacetsSearch() {
        return facets != null;
    }

    public AdcSearchRequest addField(String field) {
        if (this.fields == null) {
            this.fields = new HashSet<>();
        }

        this.fields.add(field);
        return this;
    }

    @JsonIgnore
    public boolean isIncludeFieldsEmpty() {
        return this.includeFields == null;
    }

    @JsonIgnore
    public boolean isFieldsEmpty() {
        return this.fields == null || this.fields.isEmpty();
    }

    public AdcSearchRequest addFields(String... fields) {
        Arrays.stream(fields).forEach(this::addField);

        return this;
    }

    /**
     * Copies this instance but only with the query parameters 'from', 'size', 'filters'.
     * Filters is copied by reference.
     *
     * @return the clone
     */
    public AdcSearchRequest queryClone() {
        var ret = new AdcSearchRequest();
        ret.from = from;
        ret.size = size;
        ret.filters = filters;

        return ret;
    }

    public AdcFilter getFilters() {
        return filters;
    }

    public void setFilters(AdcFilter filters) {
        this.filters = filters;
    }

    public AdcSearchRequest withFieldIn(String field, List<String> values) {
        var inContent = new PrimitiveListContent();
        inContent.setField(field);
        try {
            inContent.setValue((List) values);
        } catch (AdcException e) {
            throw new IllegalArgumentException(e);
        }

        var inFilter = new PrimitiveListContentFilter();
        inFilter.setOp("in");
        inFilter.setContent(inContent);

        if (this.filters == null) {
            this.filters = inFilter;
            return this;
        }

        var andFilter = new LogicalFilter();
        andFilter.setContent(List.of(inFilter, this.filters));
        andFilter.setOp("and");
        this.filters = andFilter;

        return this;
    }

    public IncludeField getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(IncludeField includeFields) {
        this.includeFields = includeFields;
    }

    @JsonIgnore
    public Set<String> getRequestedFilterFields() {
        var fields = new HashSet<String>();

        if (filters != null) {
            filters.loadFields(fields);
        }

        return fields;
    }

    @JsonIgnore
    public Set<String> getFiltersOperators() {
        var operators = new HashSet<String>();

        if (filters != null) {
            filters.loadOperators(operators);
        }

        return operators;
    }

    @JsonIgnore
    public AdcSearchRequest withFacets(String field) {
        this.setFacets(field);

        return this;
    }
}
