package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import pt.inesctec.adcauthmiddleware.adc.models.filters.AdcFilter;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdcSearchRequest {
  private AdcFilter filters;
  private Set<String> fields;
  private Long from;
  private Long size;
  private String format;
  private String facets;

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

  public void setFormat(String format) {
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
  public boolean isFieldsEmpty() {
    return this.fields == null || this.fields.isEmpty();
  }

  public boolean tryAddField(String field) {
    if (this.isFieldsEmpty()) {
      return false;
    }

    if (this.fields.contains(field)) {
      return false;
    }

    this.fields.add(field);
    return true;
  }

  public AdcSearchRequest addFields(String ... fields) {
    Arrays.stream(fields)
        .forEach(this::addField);

    return this;
  }

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

  public static void validate(AdcSearchRequest adcSearch, Map<String, FieldType> validFieldTypes) throws AdcException {
    var fields = adcSearch.getFields();
    if (fields != null && adcSearch.getFacets() != null) {
      throw new AdcException("Can't use 'fields' and 'facets' at the same time in request");
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
  }
}
