package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;

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

  public boolean isJsonFormat() {
    return format == null || format.equals("json");
  }

  public static AdcSearchRequest buildIdsStudySearch() {
    var ret = AdcSearchRequest.filterToIdsSearch(new AdcSearchRequest());
    ret.addField("study_title"); // TODO replace 'study_title' with 'study.study_title' when turnkey backend bug is fixed

    return ret;
  }

  private void addField(String field) {
    this.fields.add(field);
  }

  public static AdcSearchRequest filterToIdsSearch(AdcSearchRequest request) {
    var ret = new AdcSearchRequest();
    var fields = new ArrayList<>(List.of("repertoire_id", "study_id"));
    ret.setFields(fields); // TODO replace 'study_id' with 'study.study_id' when turnkey backend bug is fixed
    ret.from = request.from;
    ret.size = request.size;
    // filters

    return ret;
  }
}
