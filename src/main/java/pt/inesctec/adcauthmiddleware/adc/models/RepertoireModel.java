package pt.inesctec.adcauthmiddleware.adc.models;

import javax.validation.constraints.NotNull;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.adc.old.RepertoireSet;

/**
 * Models a repertoire response element, but with only the ID fields and the study title.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepertoireModel {
    @JsonProperty(RepertoireSet.ID_FIELD)
    private String repertoireId;

    @NotNull
    private String studyId;

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

    @JsonProperty(RepertoireSet.STUDY_BASE)
    public void unpackStudyId(Map<String, Object> study) {
        this.studyId = (String) study.get(RepertoireSet.STUDY_ID_BASE);
        this.studyTitle = (String) study.get(RepertoireSet.STUDY_TITLE_BASE);
    }

    public String getStudyTitle() {
        return studyTitle;
    }

    public void setStudyTitle(String studyTitle) {
        this.studyTitle = studyTitle;
    }
}
