package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * Models the DB's study ID to UMA ID associations.
 */
@Entity
public class Study {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * String of the Study ID provided by the ADC Service
     */
    @Column(unique = true, nullable = false)
    private String studyId;

    /**
     * String of the UMA ID provided by the UMA Authorization Service
     */
    @Column(unique = true, nullable = false)
    private String umaId;

    /**
     * Field Accessibility mappings for this particular Study
     */
    @OneToMany(
        mappedBy = "study",
        fetch = FetchType.LAZY,
        orphanRemoval = true,
        cascade = CascadeType.ALL
    )
    private List<StudyMappings> mappings;

    //@OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.REMOVE)
    //private List<Repertoire> repertoires = new ArrayList<>();

    protected Study() {}

    public Study(String studyId, String umaId) {
        this.studyId = studyId;
        this.umaId = umaId;
    }

    public String getStudyId() {
        return studyId;
    }

    public String getUmaId() {
        return umaId;
    }

    @Override
    public String toString() {
        return String.format("{studyId: %s, umaId: %s}", studyId, umaId);
    }
}
