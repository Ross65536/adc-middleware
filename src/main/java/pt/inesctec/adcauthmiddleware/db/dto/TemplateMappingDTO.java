package pt.inesctec.adcauthmiddleware.db.dto;

import java.util.ArrayList;
import java.util.List;

import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

/**
 * DTO for mapping an AccessScope with their AdcFields.
 */
public class TemplateMappingDTO {
    // Contains the AccessScope for this mapping
    private long scope;
    // Contains the AdcField IDs for the current AccessScope
    private List<Integer> fields;

    /**
     * Build a TemplateMappingDTO by providing both the access scope and the list of field for that scope.
     *
     * @param scope Scope to be added to this Mapping
     * @param fields List of ADC Fields to be mapped
     */
    public TemplateMappingDTO(long scope, List<AdcFields> fields) {
        this.scope = scope;

        for (var field : fields) {
            this.fields.add(field.getId());
        }
    }

    /**
     * Build a TemplateMappingDTO by just providing the access scope. This will create a mapping for a provided scope,
     * along with an empty list of AdcFields that you can fill in later after building this object.
     *
     * @param scope Scope to be added to this Mapping
     */
    public TemplateMappingDTO(long scope) {
        this.scope = scope;
        this.fields = new ArrayList<Integer>();
    }

    // Manually add an ADC field
    public void addField(Integer fieldId) {
        this.fields.add(fieldId);
    }

    public List<Integer> getFields() {
        return fields;
    }

    public void setFields(List<Integer> fields) {
        this.fields = fields;
    }

    public long getScope() {
        return scope;
    }

    public void setScope(long scope) {
        this.scope = scope;
    }
}
