package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Models the configuration CSV's 'class' column.
 * Also represents the different types of resources in the ADC API.
 */
public enum FieldClass {
  @JsonProperty("Repertoire")
  REPERTOIRE,
  @JsonProperty("Rearrangement")
  REARRANGEMENT;
}
