package pt.inesctec.adcauthmiddleware.db.dto;

import java.util.List;

public class PostTemplateDto {
    Long id;
    String name;
    List<PostTemplateMappingDto> mappings;

    public PostTemplateDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PostTemplateMappingDto> getMappings() {
        return mappings;
    }

    public void setMappings(List<PostTemplateMappingDto> mappings) {
        this.mappings = mappings;
    }
}
