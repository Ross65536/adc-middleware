package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.models.Templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyDto {
    Long id;
    String name;
    List<TemplateMappingDto> mappings;

    public StudyDto(Study study, List<AccessScope> scopes) {
        Map<Long, TemplateMappingDto> mapScopeFields = new HashMap<>();

        for (var mapping : study.getMappings()) {
            long scopeId = mapping.getScope().getId();

            TemplateMappingDto tmDto = mapScopeFields.getOrDefault(scopeId, new TemplateMappingDto(mapping.getScope().getId()));
            tmDto.addField(mapping.getField().getId());

            mapScopeFields.put(scopeId, tmDto);
        }

        this.id = study.getId();
        this.name = study.getStudyId();
        this.mappings = new ArrayList<TemplateMappingDto>(mapScopeFields.values());

        for (var scope: scopes) {
            boolean found = false;
            for (var mapp : this.mappings) {
                if (mapp.getScope() == scope.getId()) {
                    found = true;
                }
            }
            if (!found) {
                this.mappings.add(new TemplateMappingDto(scope.getId()));
            }
        }
    }

    public StudyDto(Study study) {
        // Map field mapping by access scope to acquire something similar to:
        // The map will be keyed by access scope ID
        // "mappings": [{
        //     "scope": <scope id>,
        //     "fields": [<field id>, ...]
        // }]
        Map<Long, TemplateMappingDto> mapScopeFields = new HashMap<>();

        for (var mapping : study.getMappings()) {
            long scopeId = mapping.getScope().getId();

            TemplateMappingDto tmDto = mapScopeFields.getOrDefault(scopeId, new TemplateMappingDto(mapping.getScope().getId()));
            tmDto.addField(mapping.getField().getId());

            mapScopeFields.put(scopeId, tmDto);
        }

        this.id = study.getId();
        this.name = study.getStudyId();
        this.mappings = new ArrayList<TemplateMappingDto>(mapScopeFields.values());
    }

    public StudyDto() {}

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

    public List<TemplateMappingDto> getMappings() {
        return mappings;
    }

    public void setMappings(List<TemplateMappingDto> mappings) {
        this.mappings = mappings;
    }
}
