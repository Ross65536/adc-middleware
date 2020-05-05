package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import pt.inesctec.adcauthmiddleware.adc.models.filters.AdcFilter;

import java.util.*;

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

}
