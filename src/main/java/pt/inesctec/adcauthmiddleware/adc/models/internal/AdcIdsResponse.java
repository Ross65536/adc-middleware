package pt.inesctec.adcauthmiddleware.adc.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementModel;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireModel;

/**
 * Models an ADC document Repertoire or Rearrangement Response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdcIdsResponse {
  @JsonProperty("Repertoire")
  private List<RepertoireModel> repertoires;

  @JsonProperty("Rearrangement")
  private List<RearrangementModel> rearrangements;

  public List<RepertoireModel> getRepertoires() {
    return repertoires;
  }

  public void setRepertoires(List<RepertoireModel> repertoires) {
    this.repertoires = repertoires;
  }

  public List<RearrangementModel> getRearrangements() {
    return rearrangements;
  }

  public void setRearrangements(List<RearrangementModel> rearrangements) {
    this.rearrangements = rearrangements;
  }
}
