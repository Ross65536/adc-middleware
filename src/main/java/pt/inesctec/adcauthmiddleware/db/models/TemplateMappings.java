package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class TemplateMappings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @ManyToOne
    @JoinColumn(name = "id_template", nullable = false)
    private Templates template;

    @ManyToOne
    @JoinColumn(name = "id_access_scope", nullable = false)
    private AccessScope scope;

    @ManyToOne
    @JoinColumn(name = "id_adc_field", nullable = false)
    private AdcFieldType field;
}
