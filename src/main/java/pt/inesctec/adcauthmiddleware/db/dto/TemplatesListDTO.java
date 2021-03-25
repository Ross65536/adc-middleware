package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.Templates;

public class TemplatesListDTO {
    Long id;
    String name;

    public TemplatesListDTO(Templates template) {
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
