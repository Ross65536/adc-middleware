package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;

public class AdcResource {
    // ID in the ADC service
    private String adcId;
    private UmaResource umaResource;
    private List<String> fieldMappings = new ArrayList<>();

    public String getAdcId() {
        return adcId;
    }

    public void setAdcId(String adcId) {
        this.adcId = adcId;
    }

    public AdcResource(UmaResource umaResource) {
        this.umaResource = umaResource;
    }

    public UmaResource getUmaResource() {
        return umaResource;
    }

    public void setUmaResource(UmaResource umaResource) {
        this.umaResource = umaResource;
    }

    public List<String> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<String> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }
}
