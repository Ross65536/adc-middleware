package pt.inesctec.adcauthmiddleware.adc.models;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.filters.AdcFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.LogicalFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.PrimitiveListContent;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.PrimitiveListContentFilter;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.config.csv.IncludeField;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

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

    /**
     * TODO: Possibly delete this function and remove everywhere where its called. It serves no purpose as validation should be done on the Repository's side
     *
     * Validates that the user request is semantically correct.
     *
     * @param adcSearch          the user's request
     * @param validFieldTypes    map of all the valid fields for the resource type of the endpoint and their corresponding types.
     * @param tsvRequestedFields the set of fields that are requested (from 'fields', 'include_fields'). Only applicable in a TSV request.
     * @throws AdcException on validation error.
     */
    public static void validate(
            AdcSearchRequest adcSearch,
            Map<String, FieldType> validFieldTypes,
            Set<String> tsvRequestedFields)
            throws AdcException {
        var fields = adcSearch.getFields();
        if (fields != null && adcSearch.getFacets() != null) {
            throw new AdcException("Can't use 'fields' and 'facets' at the same time in request");
        }

        if (adcSearch.getIncludeFields() != null && adcSearch.getFacets() != null) {
            throw new AdcException("Can't use 'include_fields' and 'facets' at the same time in request");
        }

        if (fields != null && !fields.isEmpty()) {
            for (var field : fields) {
                if (!validFieldTypes.containsKey(field)) {
                    throw new AdcException(String.format("'fields' '%s' value is not valid", field));
                }
            }
        }

        if (adcSearch.facets != null && !validFieldTypes.containsKey(adcSearch.facets)) {
            throw new AdcException(String.format("'facets' '%s' value is not valid", adcSearch.facets));
        }

        if (adcSearch.filters != null) {
            adcSearch.filters.validate("filters", validFieldTypes);
        }

        final boolean isTsv = !adcSearch.isJsonFormat();
        if (isTsv) {
            if (adcSearch.isFacetsSearch()) {
                throw new AdcException("can't return TSV format for facets");
            }

            for (var field : tsvRequestedFields) {
                if (field.contains(AdcConstants.ADC_FIELD_SEPERATOR)) {
                    throw new AdcException(
                            String.format("TSV: The field %s requested cannot be a nested document", field));
                }
            }
        }
    }

    /**
     * Setup the ADC request and build the mapper for a regular listing.
     *
     * @param resourceId   the resource's ID field.
     * @param fieldClass   the resource type.
     * @param umaResources the UMA resources and scopes.
     * @return the UMA ID to permitted fields mapper
     */
    //adcRegularSearchSetup
    public Function<String, Set<String>> searchSetup(
        FieldClass fieldClass,
        String resourceId,
        Collection<UmaResource> umaResources,
        CsvConfig csvConfig) {
        final Set<String> allRequestedFields = this.getRequestedFields(fieldClass, csvConfig);
        final Set<String> filtersFields = this.getFiltersFields();

        if (!allRequestedFields.contains(resourceId)) {
            this.addField(resourceId);
        }

        return UmaUtils.buildFieldMapper(umaResources, fieldClass, csvConfig).andThen(
            fields -> {
                // don't return resources where the access level does not match the one in the
                // filters, in order to avoid information leaks
                if (Sets.difference(filtersFields, fields).isEmpty()) {
                    return fields;
                }

                // Return an Empty Set
                Set<String> EmptySet = ImmutableSet.of();
                return EmptySet;
            }).andThen(set -> Sets.intersection(set, allRequestedFields));
    }

    @JsonIgnore
    /**
     * Returns the UMA scopes for the fields in this Request.
     * The considered parameters are: "facets", "fields", "include_fields", and "filters".
     * Filters operators can reference a field for the search and these are the fields considered.
     *
     * @param adcSearch  the ADC query
     * @param fieldClass the resource type
     * @return the UMA scopes.
     */
    public Set<String> getUmaScopes(FieldClass fieldClass, CsvConfig csvConfig) {
        final Set<String> requestedFields =
            this.isFacetsSearch() ?
                Set.of(this.getFacets()) : this.getRequestedFields(fieldClass, csvConfig);
        final Set<String> filtersFields = this.getFiltersFields();
        final Set<String> allConsideredFields = Sets.union(requestedFields, filtersFields);

        // empty set returned means only public fields requested
        return csvConfig.getUmaScopes(fieldClass, allConsideredFields);
    }

    /**
     * Get the fields that correspond to this Request, non-facets. Facets presence should be checked previously.
     * Only the "fields" and "include_fields" parameters are considered. If both empty all of the resource's fields are returned.
     *
     * @param fieldClass the resource type
     * @return the set of fields that were requested.
     */
    public Set<String> getRequestedFields(FieldClass fieldClass, CsvConfig csvConfig) {
        final Set<String> fields = this.isFieldsEmpty() ? Set.of() : this.getFields();
        final Set<String> includeFields = this.isIncludeFieldsEmpty() ?
            Set.of() : csvConfig.getFields(fieldClass, this.getIncludeFields());

        final Set<String> requestedFields = Sets.union(fields, includeFields);

        return new HashSet<>(
            requestedFields.isEmpty()
                ? csvConfig.getFieldsTypes(fieldClass).keySet()
                : requestedFields);
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

    @JsonIgnore
    public void unsetFormat() {
        this.format = null;
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
    public Set<String> getFiltersFields() {
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
