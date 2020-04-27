package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RearrangementIds {
  @JsonProperty("repertoire_id")
  @NotNull
  private String repertoireId;

  @JsonProperty("rearrangement_id")
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
