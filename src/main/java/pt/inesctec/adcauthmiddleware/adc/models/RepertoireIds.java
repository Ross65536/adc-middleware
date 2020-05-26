package pt.inesctec.adcauthmiddleware.adc.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.validation.constraints.NotNull;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RepertoireIds {
  @JsonProperty(AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD)
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

  @JsonProperty(AdcConstants.REPERTOIRE_STUDY_BASE)
  public void unpackStudyId(Map<String, Object> study) {
    this.studyId = (String) study.get(AdcConstants.REPERTOIRE_STUDY_ID_BASE);
    this.studyTitle = (String) study.get(AdcConstants.REPERTOIRE_STUDY_TITLE_BASE);
  }

  public String getStudyTitle() {
    return studyTitle;
  }

  public void setStudyTitle(String studyTitle) {
    this.studyTitle = studyTitle;
  }
}
