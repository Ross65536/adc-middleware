package pt.inesctec.adcauthmiddleware.adc.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Models an ADC document Facets response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdcFacetsResponse {
  @JsonProperty("Facet")
  private List<Map<String, Object>> facets;

  public List<Map<String, Object>> getFacets() {
    return facets;
  }

  public void setFacets(List<Map<String, Object>> facets) {
    this.facets = facets;
  }
}
