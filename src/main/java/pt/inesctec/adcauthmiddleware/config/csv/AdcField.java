package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.NonNull;
import pt.inesctec.adcauthmiddleware.adc.AdcUtils;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdcField {

  public enum Class {
    @JsonProperty("Repertoire")
    REPERTOIRE,
    @JsonProperty("Rearrangement")
    REARRANGEMENT;
  };

  public enum UmaScope {
    @JsonProperty(AdcUtils.PUBLIC_ACCESS_LEVEL)
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
  @JsonProperty("uma_scope")
  private UmaScope umaScope;

  public void setUmaScope(UmaScope umaScope) {
    this.umaScope = umaScope;
  }

  public UmaScope getUmaScope() {
    return umaScope;
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
