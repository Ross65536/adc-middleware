package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FieldClass {
  @JsonProperty("Repertoire")
  REPERTOIRE,
  @JsonProperty("Rearrangement")
  REARRANGEMENT;
}
