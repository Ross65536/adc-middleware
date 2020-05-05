package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CsvField {
  @JsonProperty("class")
  @NotNull
  private FieldClass fieldClass;

  @JsonProperty("field")
  @NotNull
  private String field;

  @NotNull
  @JsonProperty("access_scope")
  private AccessScope accessScope;

  @NotNull
  @JsonProperty("field_type")
  private FieldType fieldType;

  public void setAccessScope(AccessScope accessScope) {
    this.accessScope = accessScope;
  }

  public AccessScope getAccessScope() {
    return accessScope;
  }

  public FieldClass getFieldClass() {
    return fieldClass;
  }

  public void setFieldClass(FieldClass fieldClass) {
    this.fieldClass = fieldClass;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public FieldType getFieldType() {
    return fieldType;
  }

  public void setFieldType(FieldType fieldType) {
    this.fieldType = fieldType;
  }
}
