package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;

/**
 * Class for representing the state of an ADC Resource.
 * Maintains its UMA Resource state and Field Mappings for controlling accessibility.
 *
 */
public class AdcResource {
    // UMA Resource corresponding to this Resource. May be Null, in case we're dealing with a
    // request that doesn't involve UMA processing.
    private UmaResource umaResource;

    // Field Mapping for controlling accessibility - List fields that are accessible on this resource
    private List<String> fieldMappings = new ArrayList<>();

    public AdcResource() {}

    /**
     * Initialize Resource using only its UMA state.
     *
     * @param umaResource UmaResource object
     */
    public AdcResource(UmaResource umaResource) {
        this.umaResource = umaResource;
    }

    /**
     * Initialize Resource using only determined Field Mappings.
     * Used in case the Resource doesn't have an UMA state.
     * I.e. the user didn't have permission to access it,
     * so it will contain public mappings
     *
     * @param fieldMappings List of Accessible fields
     */
    public AdcResource(List<String> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public UmaResource getUmaResource() {
        return umaResource;
    }

    public void setUmaResource(UmaResource umaResource) {
        this.umaResource = umaResource;
    }

    public Set<String> getFieldMappings() {
        return new HashSet<>(fieldMappings);
    }

    public void setFieldMappings(List<String> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }
}
