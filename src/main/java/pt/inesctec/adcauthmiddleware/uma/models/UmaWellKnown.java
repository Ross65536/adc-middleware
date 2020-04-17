package pt.inesctec.adcauthmiddleware.uma.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaWellKnown {

  @JsonProperty("permission_endpoint")
  @NotNull
  private String permissionEndpoint;

  @JsonProperty("introspection_endpoint")
  @NotNull
  private String introspectionEndpoint;

  @JsonProperty("token_endpoint")
  @NotNull
  private String tokenEndpoints;

  @NotNull
  private String issuer;

  @JsonProperty("resource_registration_endpoint")
  @NotNull
  private String resourceRegistrationEndpoint;

  public String getPermissionEndpoint() {
    return permissionEndpoint;
  }

  public void setPermissionEndpoint(String permissionEndpoint) {
    this.permissionEndpoint = permissionEndpoint;
  }

  public String getIntrospectionEndpoint() {
    return introspectionEndpoint;
  }

  public void setIntrospectionEndpoint(String introspectionEndpoint) {
    this.introspectionEndpoint = introspectionEndpoint;
  }

  public String getTokenEndpoints() {
    return tokenEndpoints;
  }

  public void setTokenEndpoints(String tokenEndpoints) {
    this.tokenEndpoints = tokenEndpoints;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getResourceRegistrationEndpoint() {
    return resourceRegistrationEndpoint;
  }

  public void setResourceRegistrationEndpoint(String resourceRegistrationEndpoint) {
    this.resourceRegistrationEndpoint = resourceRegistrationEndpoint;
  }
}
