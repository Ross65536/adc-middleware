package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;

import java.util.HashMap;
import java.util.Map;

public enum AccessScope {
  @JsonProperty(AdcConstants.PUBLIC_ACCESS_SCOPE)
  PUBLIC,
  @JsonProperty(AdcConstants.STATISTICS_UMA_SCOPE)
  STATISTICS,
  @JsonProperty(AdcConstants.REPERTOIRE_UMA_SCOPE)
  REPERTOIRE,
  @JsonProperty(AdcConstants.SEQUENCE_UMA_SCOPE)
  SEQUENCE;

  private static Map<String, AccessScope> ScopeMapping = ImmutableMap.of(
      AdcConstants.PUBLIC_ACCESS_SCOPE, PUBLIC,
      AdcConstants.STATISTICS_UMA_SCOPE, STATISTICS,
      AdcConstants.REPERTOIRE_UMA_SCOPE, REPERTOIRE,
      AdcConstants.SEQUENCE_UMA_SCOPE, SEQUENCE
  );

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

  public static AccessScope fromString(String value) {
    var scope = ScopeMapping.get(value);

    if (scope == null) {
      throw new IllegalArgumentException("No mapping for scope: " + value);
    }

    return scope;
  }
}
