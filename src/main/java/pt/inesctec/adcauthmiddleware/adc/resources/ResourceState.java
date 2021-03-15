package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pt.inesctec.adcauthmiddleware.adc.resources.AdcResource;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;

/**
 * Resource State Manager
 * Class for managing common UMA States across requests.
 *
 * Resource Loading workflow:
 *  - Determine UMA IDs that identify the ADC Resource
 *  - Using the UMA ID, determine the Scopes of those Resources
 *  - If the request is non-public
 *    - Determine with the UMA Service the list of UmaResources
 *    - These UmaResources contain both their UMA ID and their specific scopes (that have been allow to the user
 */
public class ResourceState {
    private boolean isEnabled = false;

    // Set of resources requested and to be inquired with the UMA Service
    private Set<String> umaIds = new HashSet<>();

    // Map of AdcResources
    // Map[UMA ID] => AdcResource
    private Map<String, AdcResource> resources = new HashMap<>();

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Set<String> getUmaIds() {
        return umaIds;
    }

    public void setUmaIds(Set<String> umaIds) {
        this.umaIds = umaIds;
    }

    public Map<String, AdcResource> getResources() {
        return resources;
    }

    public void setResources(Map<String, AdcResource> resources) {
        this.resources = resources;
    }

    /**
     * Set Resources from a list of UmaResources. Will map resources in the following format:
     *
     * Map[UMA ID] => AdcResource
     *
     * @param resources list of UmaResources
     */
    public void setFromUmaResources(List<UmaResource> resources) {
        this.resources = resources.stream().collect(
            Collectors.toMap(
                UmaResource::getUmaId, // key   -> UMA ID
                AdcResource::new       // value -> AdcResource
            )
        );
    }

    public void addResource(AdcResource resource) {
        this.resources.put(resource.getUmaResource().getUmaId(), resource);
    }
}
