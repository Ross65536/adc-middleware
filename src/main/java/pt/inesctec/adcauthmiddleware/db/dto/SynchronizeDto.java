package pt.inesctec.adcauthmiddleware.db.dto;

import java.util.List;

public class SynchronizeDto {

    private String ownerId;

    private List<String> studies;

    public SynchronizeDto(String ownerId, List<String> studies) {
        this.ownerId = ownerId;
        this.studies = studies;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<String> getStudies() {
        return studies;
    }

    public void setStudies(List<String> studies) {
        this.studies = studies;
    }
}
