package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RepertoireIds {
  @JsonProperty("repertoire_id")
  private String repertoireId;

  @NotNull private String studyId;

  private String studyTitle;

  public String getRepertoireId() {
    return repertoireId;
  }

  public void setRepertoireId(String repertoireId) {
    this.repertoireId = repertoireId;
  }

  public String getStudyId() {
    return studyId;
  }

  @JsonProperty("study")
  public void unpackStudyId(Map<String, String> study) {
    this.studyId = study.get("study_id");
    this.studyTitle = study.get("study_title");
  }

  public String getStudyTitle() {
    return studyTitle;
  }

  public void setStudyTitle(String studyTitle) {
    this.studyTitle = studyTitle;
  }
}
