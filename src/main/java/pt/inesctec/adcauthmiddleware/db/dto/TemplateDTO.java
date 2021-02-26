package pt.inesctec.adcauthmiddleware.db.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.inesctec.adcauthmiddleware.db.models.Templates;

public class TemplateDTO {
    Long id;
    String name;
    List<TemplateMappingDTO> mappings;

    public TemplateDTO(Templates template) {
        // Map field mapping by access scope to acquire something similar to:
        // The map will be keyed by access scope ID
        // "mappings": [{
        //		"scope": <scope id>,
        //		"fields": [<field id>, ...]
        //	}]
        Map<Long, TemplateMappingDTO> mapScopeFields = new HashMap<>();

        for (var mapping : template.getMappings()) {
            long scopeId = mapping.getScope().getId();

            TemplateMappingDTO tmDto = mapScopeFields.getOrDefault(scopeId, new TemplateMappingDTO(mapping.getScope()));
            tmDto.addField(mapping.getField().getId());

            mapScopeFields.put(scopeId, tmDto);
        }

        this.id = template.getId();
        this.name = template.getName();
        this.mappings = new ArrayList<TemplateMappingDTO>(mapScopeFields.values());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TemplateMappingDTO> getMappings() {
        return mappings;
    }
}
