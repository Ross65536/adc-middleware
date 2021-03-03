package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class StudyMappings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_study", nullable = false)
    private Study study;

    @ManyToOne
    @JoinColumn(name = "id_access_scope", nullable = false)
    private AccessScope scope;

    @ManyToOne
    @JoinColumn(name = "id_adc_field", nullable = false)
    private AdcFields field;

    /**
     * Create a StudyMapping from a TemplateMapping.
     *
     * @param templateMapping Base mapping from a TemplateMapping
     */
    public StudyMappings(TemplateMappings templateMapping, Study study) {
        this.scope = templateMapping.getScope();
        this.field = templateMapping.getField();
    }
}
