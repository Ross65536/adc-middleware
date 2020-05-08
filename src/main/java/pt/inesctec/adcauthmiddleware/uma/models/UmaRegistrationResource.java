package pt.inesctec.adcauthmiddleware.uma.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UmaRegistrationResource {
  @JsonProperty("_id")
  private String id;

  private String name;
  private String type;

  @JsonProperty("resource_scopes")
  private Set<String> resourceScopes;

  // keycloak specific
  // should probably be set differently
  private String owner;
  private Boolean ownerManagedAccess = true;

  public UmaRegistrationResource() {}

  public UmaRegistrationResource(String name, String type, Set<String> resourceScopes) {
    this.name = name;
    this.type = type;
    this.resourceScopes = resourceScopes;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<String> getResourceScopes() {
    return resourceScopes;
  }

  public void setResourceScopes(Set<String> resourceScopes) {
    this.resourceScopes = resourceScopes;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Boolean getOwnerManagedAccess() {
    return ownerManagedAccess;
  }

  public void setOwnerManagedAccess(Boolean ownerManagedAccess) {
    this.ownerManagedAccess = ownerManagedAccess;
  }

  @Override
  public String toString() {
    return String.format(
        "{name: '%s', type: '%s', scopes: %s, owner: '%s'}",
        this.name, this.type, CollectionsUtils.toString(this.resourceScopes), this.owner);
  }
}
