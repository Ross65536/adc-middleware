package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;

public enum AccessScope {
  @JsonProperty(AdcConstants.PUBLIC_ACCESS_SCOPE)
  PUBLIC,
  @JsonProperty(AdcConstants.STATISTICS_UMA_SCOPE)
  STATISTICS,
  @JsonProperty(AdcConstants.REPERTOIRE_UMA_SCOPE)
  REPERTOIRE,
  @JsonProperty(AdcConstants.SEQUENCE_UMA_SCOPE)
  SEQUENCE;

  @Override
  public String toString() {
    try {
      return AccessScope.class.getField(this.name())
          .getAnnotation(JsonProperty.class)
          .value();
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Invalid annotation config for AccessScope");
    }
  }
}
