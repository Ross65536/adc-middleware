package pt.inesctec.adcauthmiddleware.config.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

/**
 * Model's the configuration CSV rows.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CsvField {
  @JsonProperty("class")
  @NotNull
  private FieldClass fieldClass;

  @JsonProperty("field")
  @NotNull
  private String field;

  @JsonProperty("access_scope")
  private String accessScope;

  @NotNull
  @JsonProperty("field_type")
  private FieldType fieldType;

  @NotNull private boolean isPublic;

  @JsonProperty("include_fields")
  private IncludeField includeField;

  private static final String ValidScopePattern = "^[\\w_]*$";

  public void setAccessScope(String accessScope) {
    if (accessScope != null) {
      accessScope = accessScope.trim();

      if (!accessScope.matches(ValidScopePattern)) {
        throw new IllegalArgumentException(
            "Invalid field mapping CSV access scope: "
                + accessScope
                + " must match pattern: "
                + ValidScopePattern);
      }

      if (accessScope.length() == 0) {
        accessScope = null;
      }
    }

    this.accessScope = accessScope;
  }

  public boolean isEmptyScope() {
    return this.accessScope == null;
  }

  public String getAccessScope() {
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

  public boolean isPublic() {
    return isPublic;
  }

  @JsonProperty("protection")
  public void setPublic(String protectionString) {
    switch (protectionString) {
      case "public":
        this.isPublic = true;
        return;
      case "protected":
        this.isPublic = false;
        return;
      default:
        throw new IllegalArgumentException(
            "Invalid field mapping CSV protection value: " + protectionString);
    }
  }

  public IncludeField getIncludeField() {
    return includeField;
  }

  public void setIncludeField(IncludeField includeField) {
    this.includeField = includeField;
  }
}
