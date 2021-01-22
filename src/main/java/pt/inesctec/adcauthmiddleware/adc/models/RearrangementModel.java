package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;

import javax.validation.constraints.NotNull;

/**
 * Models a rearrangement response but only with the ID fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RearrangementModel {
  @JsonProperty(AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD)
  @NotNull
  private String repertoireId;

  @JsonProperty(AdcConstants.REARRANGEMENT_REARRANGEMENT_ID_FIELD)
  private String rearrangementId;

  public String getRepertoireId() {
    return repertoireId;
  }

  public void setRepertoireId(String repertoireId) {
    this.repertoireId = repertoireId;
  }

  public String getRearrangementId() {
    return rearrangementId;
  }

  public void setRearrangementId(String rearrangementId) {
    this.rearrangementId = rearrangementId;
  }
}
