package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.Templates;

public class TemplatesListDto {
    Long id;
    String name;

    public TemplatesListDto(Templates template) {
        this.id = template.getId();
        this.name = template.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
