package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pt.inesctec.adcauthmiddleware.adc.models.filters.AdcFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.LogicalFilter;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.PrimitiveListContent;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.filters.PrimitiveListContentFilter;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;

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

  private static final Set<String> SupportedFormats = Set.of("json", "tsv");

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

  public AdcSearchRequest addFields(String... fields) {
    Arrays.stream(fields).forEach(this::addField);

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

  public static void validate(AdcSearchRequest adcSearch, Map<String, FieldType> validFieldTypes)
      throws AdcException {
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
}
