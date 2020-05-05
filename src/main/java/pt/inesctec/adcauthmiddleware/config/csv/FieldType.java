package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FieldType {
  @JsonProperty("string")
  STRING,
  @JsonProperty("boolean")
  BOOLEAN,
  @JsonProperty("number")
  NUMBER,
  @JsonProperty("integer")
  INTEGER,
  @JsonProperty("array_string")
  ARRAY_STRING
}
