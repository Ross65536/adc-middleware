package pt.inesctec.adcauthmiddleware.uma;

import java.util.List;
import java.util.Set;

import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;

/**
 * Class for managing common UMA States across requests.
 */
public class UmaStateOld {
    private boolean isEnabled = false;
    private Set<String> scopes;
    private Set<String> umaIds;
    private List<UmaResource> resources;

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Set<String> getUmaIds() {
        return umaIds;
    }

    public void setUmaIds(Set<String> umaIds) {
        this.umaIds = umaIds;
    }

    public List<UmaResource> getResources() {
        return resources;
    }

    public void setResources(List<UmaResource> resources) {
        this.resources = resources;
    }
}
