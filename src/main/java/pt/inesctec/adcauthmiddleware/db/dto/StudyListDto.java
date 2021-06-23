package pt.inesctec.adcauthmiddleware.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import pt.inesctec.adcauthmiddleware.db.models.Study;

public class StudyListDto {
    long id;
    String name;
    @JsonProperty("uma_id")
    String umaId;

    public StudyListDto(Study study) {
        this.id = study.getId();
        this.name = study.getStudyId();
        this.umaId = study.getUmaId();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUmaId() {
        return umaId;
    }
}
