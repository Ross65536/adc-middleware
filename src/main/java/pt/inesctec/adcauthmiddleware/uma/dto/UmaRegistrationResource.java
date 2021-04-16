package pt.inesctec.adcauthmiddleware.uma.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

/**
 * Models a UMA resource used when creating a resource. Has some fields specific to Keycloak.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UmaRegistrationResource {
    @JsonProperty("_id")
    private String id;

    private String name;
    private String type;
    @JsonIgnore
    private Set<String> resourceScopes;

    @JsonIgnore
    private String owner;
    private Boolean ownerManagedAccess;

    private UmaResourceAttributes attributes;

    public UmaRegistrationResource() {
    }

    public UmaRegistrationResource(String name, String type, Set<String> resourceScopes) {
        this.name = name;
        this.type = type;
        this.resourceScopes = resourceScopes; // keycloak will delete type if not present here
        this.ownerManagedAccess = true;
    }

    public UmaRegistrationResource(String name, String type, Set<String> resourceScopes, UmaResourceAttributes attributes) {
        this.name = name;
        this.type = type;
        this.resourceScopes = resourceScopes;
        this.attributes = attributes;
        this.ownerManagedAccess = true;
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

    @JsonProperty("resource_scopes")
    public Set<String> getResourceScopes() {
        return resourceScopes;
    }

    @JsonIgnore
    public void setResourceScopes(Set<String> resourceScopes) {
        this.resourceScopes = resourceScopes;
    }

    @JsonProperty("resource_scopes") // Keycloak specific, keycloak doesn't follow spec
    public void setResourceScopes(List<Map<String, String>> resourceScopes) {
        String errorMsg =
            "Expected Keycloak response 'resource_scopes' objects to contain 'name' field";

        this.resourceScopes = resourceScopes.stream().map(map -> {
            if (!map.containsKey("name")) {
                throw new IllegalArgumentException(errorMsg);
            }

            return map.get("name");
        }).collect(Collectors.toSet());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }

    @JsonIgnore
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JsonProperty
    public void setOwner(Map<String, String> owner) {
        if (!owner.containsKey("id")) {
            throw new IllegalArgumentException(
                "Expected Keycloak response 'owner' to object containing 'id' field");
        }

        this.owner = owner.get("id");
    }

    public Boolean getOwnerManagedAccess() {
        return ownerManagedAccess;
    }

    public void setOwnerManagedAccess(Boolean ownerManagedAccess) {
        this.ownerManagedAccess = ownerManagedAccess;
    }

    public UmaResourceAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(UmaResourceAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return String.format(
            "{name: '%s', type: '%s', scopes: %s, owner: '%s'}",
            this.name, this.type, CollectionsUtils.toString(this.resourceScopes), this.owner);
    }
}
