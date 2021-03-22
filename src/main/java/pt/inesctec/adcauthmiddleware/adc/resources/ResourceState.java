package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;

/**
 * Resource State Manager
 * Class for managing common ADC Resources, UMA States and Field accessibility across requests.
 *
 * Resource Loading workflow:
 * <ul>
 *   <li>Determine UMA IDs that identify the ADC Resource</li>
 *   <li>Using the UMA ID, determine the Scopes of those Resources</li>
 *   <li>If the request is non-public</li>
 *   <ul>
 *     <li>Determine with the UMA Service the list of UmaResources</li>
 *     <li>These UmaResources contain both their UMA ID and their specific scopes (that have been allow to the user</li>
 *   </ul>
 *  </ul>
 */
public class ResourceState {
    // Defines if the state should trigger the UMA workflow.
    //  * If Disabled - Will map the resource to only output fields that have been defined as public
    //  * If Enabled  - Will follow the UMA workflow normally
    private boolean umaEnabled = false;

    // Set of UMA IDs requested and to be inquired with the UMA Service
    // The size of the Set may not match the number of resources returned by the UMA Service.
    // This happens when, for example, the user doesn't have permissions to access a certain UMA ID.
    private Set<String> umaIds = new HashSet<>();

    // Set of UMA scopes requested and to be inquired with the UMA Service
    // Follows the same principle of the Set above
    private Set<String> scopes = new HashSet<>();

    // Map of AdcResources
    //   Map[UMA ID] => AdcResource
    private Map<String, AdcResource> resources = new HashMap<>();

    public boolean isUmaEnabled() {
        return umaEnabled;
    }

    public void setUmaEnabled(boolean umaEnabled) {
        this.umaEnabled = umaEnabled;
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

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
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

    Function<String, Set<String>> setupFieldMapper() {
        return umaId -> {
            if (!this.getResources().containsKey(umaId)) {
                return Set.of();
            }

            return this.getResources().get(umaId).getFieldMappings();
        };
    }
}
