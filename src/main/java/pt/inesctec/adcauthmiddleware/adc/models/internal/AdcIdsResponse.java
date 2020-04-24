package pt.inesctec.adcauthmiddleware.adc.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementIds;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireIds;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdcIdsResponse {
  @JsonProperty("Repertoire")
  private List<RepertoireIds> repertoires;

  @JsonProperty("Rearrangement")
  private List<RearrangementIds> rearrangements;

  public List<RepertoireIds> getRepertoires() {
    return repertoires;
  }

  public void setRepertoires(List<RepertoireIds> repertoires) {
    this.repertoires = repertoires;
  }

  public List<RearrangementIds> getRearrangements() {
    return rearrangements;
  }

  public void setRearrangements(List<RearrangementIds> rearrangements) {
    this.rearrangements = rearrangements;
  }
}
