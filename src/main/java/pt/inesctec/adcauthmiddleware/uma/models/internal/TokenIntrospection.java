package pt.inesctec.adcauthmiddleware.uma.models.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

/**
 * Models the result of a UMA RPT token introspection response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenIntrospection {
    private boolean active;
    private List<UmaResource> permissions;
    private Set<String> roles;

    @JsonProperty("realm_access")
    //@SuppressWarnings("unchecked")
    private void rolesDeserializer(Map<String, Set<String>> realmAccess) {
        this.roles = realmAccess.getOrDefault("roles", Collections.<String>emptySet());
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<UmaResource> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<UmaResource> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
