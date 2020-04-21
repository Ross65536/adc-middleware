package pt.inesctec.adcauthmiddleware.uma.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UmaResource {

  @JsonProperty("resource_id")
  private String umaResourceId;

  @JsonProperty("resource_scopes")
  private List<String> scopes;

  public UmaResource(String umaResourceId, String ... scopes) {
    this.umaResourceId = umaResourceId;
    this.scopes = List.of(scopes);
  }

  public String getUmaResourceId() {
    return umaResourceId;
  }

  public List<String> getScopes() {
    return scopes;
  }
}
