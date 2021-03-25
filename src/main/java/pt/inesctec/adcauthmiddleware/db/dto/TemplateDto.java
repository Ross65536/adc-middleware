package pt.inesctec.adcauthmiddleware.db.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.inesctec.adcauthmiddleware.db.models.Templates;

public class TemplateDto {
    Long id;
    String name;
    List<TemplateMappingDto> mappings;

    public TemplateDto(Templates template) {
        // Map field mapping by access scope to acquire something similar to:
        // The map will be keyed by access scope ID
        // "mappings": [{
        //     "scope": <scope id>,
        //     "fields": [<field id>, ...]
        // }]
        Map<Long, TemplateMappingDto> mapScopeFields = new HashMap<>();

        for (var mapping : template.getMappings()) {
            long scopeId = mapping.getScope().getId();

            TemplateMappingDto tmDto = mapScopeFields.getOrDefault(scopeId, new TemplateMappingDto(mapping.getScope().getId()));
            tmDto.addField(mapping.getField().getId());

            mapScopeFields.put(scopeId, tmDto);
        }

        this.id = template.getId();
        this.name = template.getName();
        this.mappings = new ArrayList<TemplateMappingDto>(mapScopeFields.values());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TemplateMappingDto> getMappings() {
        return mappings;
    }
}
