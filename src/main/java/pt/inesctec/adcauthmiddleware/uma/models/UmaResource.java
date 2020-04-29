package pt.inesctec.adcauthmiddleware.uma.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaResource {

  @JsonProperty("resource_id")
  private String umaResourceId;

  @JsonProperty("resource_scopes")
  private Set<String> scopes;

  public UmaResource() { }

  public UmaResource(String umaResourceId, String ... scopes) {
    this(umaResourceId, new HashSet<>(List.of(scopes)));
  }

  public UmaResource(String umaId, Set<String> umaScopes) {
    this.umaResourceId = umaId;
    this.scopes = umaScopes;
  }

  public String getUmaResourceId() {
    return umaResourceId;
  }

  public Set<String> getScopes() {
    return scopes;
  }
}
