package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

public class AdcFieldsDto {
    private long id;
    private String name;
    private long class_id;

    public AdcFieldsDto(AdcFields adcFields) {
        this.id = adcFields.getId();
        this.name = adcFields.getName();
        this.class_id = adcFields.getType().getId();
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

    public void setName(String name) { this.name = name; }

    public long getClass_id() { return class_id; }

    public void setClass_id(long class_id) { this.class_id = class_id; }
}
