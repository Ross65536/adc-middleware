package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.Study;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostStudyDto {
    Long id;
    String name;
    List<PostTemplateMappingDto> mappings;

    public PostStudyDto() {}

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
