package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

public class AdcFieldDTO {
    private long id;
    private String name;

    public AdcFieldDTO(AdcFields adcFields) {
        this.id = adcFields.getId();
        this.name = adcFields.getName();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
