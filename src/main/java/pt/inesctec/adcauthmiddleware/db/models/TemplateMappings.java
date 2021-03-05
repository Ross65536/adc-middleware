package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "template_mappings")
public class TemplateMappings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "id_template", nullable = false)
    private Templates template;

    @ManyToOne
    @JoinColumn(name = "id_access_scope", nullable = false)
    private AccessScope scope;

    @ManyToOne
    @JoinColumn(name = "id_adc_field", nullable = false)
    private AdcFields field;

    protected TemplateMappings() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Templates getTemplate() {
        return template;
    }

    public void setTemplate(Templates template) {
        this.template = template;
    }

    public AccessScope getScope() {
        return scope;
    }

    public void setScope(AccessScope scope) {
        this.scope = scope;
    }

    public AdcFields getField() {
        return field;
    }

    public void setField(AdcFields field) {
        this.field = field;
    }
}
