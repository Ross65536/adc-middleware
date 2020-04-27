package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdcSearchRequest {
  // filters
  private List<String> fields;
  private Long from;
  private Long size;
  private String format;
  private String facets;

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
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

  private AdcSearchRequest addField(String field) {
    if (this.fields == null) {
      this.fields = new ArrayList<>();
    }

    this.fields.add(field);
    return this;
  }

  private AdcSearchRequest queryClone() {
    var ret = new AdcSearchRequest();
    ret.from = from;
    ret.size = size;
    //TODO copy filters

    return ret;
  }

  public static AdcSearchRequest buildIdsRearrangementSearch() {
    return AdcSearchRequest.filterRearrangementIdsSearch(new AdcSearchRequest());
  }

  public static AdcSearchRequest buildIdsRepertoireSearch() {
    return AdcSearchRequest.filterRepertoireIdsSearch(new AdcSearchRequest())
        .addField("study_title"); // TODO replace 'study_title' with 'study.study_title' when turnkey backend bug is fixed
  }

  public static AdcSearchRequest filterRearrangementIdsSearch(AdcSearchRequest request) {
    return request.queryClone()
        .addField("repertoire_id")
        .addField("rearrangement_id");
  }

  public static AdcSearchRequest filterRepertoireIdsSearch(AdcSearchRequest request) {
    return request.queryClone()
        .addField("study_id") // TODO replace 'study_id' with 'study.study_id' when turnkey backend bug is fixed
        .addField("repertoire_id");
  }
}
