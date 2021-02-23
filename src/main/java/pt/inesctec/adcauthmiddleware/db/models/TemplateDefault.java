package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
public class TemplateDefault {
    @Id
    long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_template", nullable = false)
    private Templates template;
}
