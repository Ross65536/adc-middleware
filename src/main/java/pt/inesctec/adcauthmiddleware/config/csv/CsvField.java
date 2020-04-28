package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.adc.AdcUtils;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CsvField {

  public enum Class {
    @JsonProperty("Repertoire")
    REPERTOIRE,
    @JsonProperty("Rearrangement")
    REARRANGEMENT;
  };

  public enum AccessScope {
    @JsonProperty(AdcUtils.PUBLIC_ACCESS_SCOPE)
    PUBLIC,
    @JsonProperty(AdcUtils.STATISTICS_UMA_SCOPE)
    STATISTICS,
    @JsonProperty(AdcUtils.REPERTOIRE_UMA_SCOPE)
    REPERTOIRE,
    @JsonProperty(AdcUtils.SEQUENCE_UMA_SCOPE)
    SEQUENCE;
  };

  @JsonProperty("class")
  @NotNull
  private Class fieldClass;

  @JsonProperty("field")
  @NotNull
  private String field;

  @NotNull
  @JsonProperty("access_scope")
  private AccessScope accessScope;

  public void setAccessScope(AccessScope accessScope) {
    this.accessScope = accessScope;
  }

  public AccessScope getAccessScope() {
    return accessScope;
  }

  public Class getFieldClass() {
    return fieldClass;
  }

  public void setFieldClass(Class fieldClass) {
    this.fieldClass = fieldClass;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }
}
