package pt.inesctec.adcauthmiddleware.db.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

/**
 * DTO for mapping an AccessScope with their AdcFields
 */
public class TemplateMappingDTO {
    // Contains the AccessScope for this mapping
    private AccessScopeDTO scope;
    // Contains the AdcField IDs for the current AccessScope
    private List<Long> fields;

    /**
     * Build a TemplateMappingDTO by providing both the access scope and the list of field for that scope simultaneously
     *
     * @param scope
     * @param fields
     */
    public TemplateMappingDTO(AccessScope scope, List<AdcFields> fields) {
        this.scope = new AccessScopeDTO(scope);

        for (var field : fields) {
            this.fields.add(field.getId());
        }
    }

    /**
     * Build a TemplateMappingDTO by just providing the access scope. This will create a mapping for a provided scope,
     * along with an empty list of AdcFields that you can fill in later after building this object.
     *
     * @param scope
     */
    public TemplateMappingDTO(AccessScope scope) {
        this.scope = new AccessScopeDTO(scope);
        this.fields = new ArrayList<>();
    }

    // Manually add an
    public boolean addField(long fieldId) {
        return this.fields.add(fieldId);
    }

    public List<Long> getFields() {
        return fields;
    }

    public void setFields(List<Long> fields) {
        this.fields = fields;
    }

    public AccessScopeDTO getScope() {
        return scope;
    }

    public void setScope(AccessScopeDTO scope) {
        this.scope = scope;
    }
}
