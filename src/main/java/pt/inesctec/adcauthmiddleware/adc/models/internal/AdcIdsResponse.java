package pt.inesctec.adcauthmiddleware.adc.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireIds;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdcIdsResponse {
  @JsonProperty("Repertoire")
  private List<RepertoireIds> repertoires;

  public List<RepertoireIds> getRepertoires() {
    return repertoires;
  }

  public void setRepertoires(List<RepertoireIds> repertoires) {
    this.repertoires = repertoires;
  }
}
