package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Models the configuration CSV's 'field_type' column. Represents the different field value types in the ADC API.
 */
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
