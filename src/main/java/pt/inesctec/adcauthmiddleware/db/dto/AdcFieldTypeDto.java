package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.AdcFieldType;

public class AdcFieldTypeDto {
    private int id;
    private String name;

    public AdcFieldTypeDto(AdcFieldType adcFieldType) {
        this.id = adcFieldType.getId();
        this.name = adcFieldType.getName();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
