package pt.inesctec.adcauthmiddleware.db.models;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Models the DB's study ID to UMA ID associations.
 */
@Entity
public class Study {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String studyId;

  @Column(unique = true, nullable = false)
  private String umaId;

  @Column(nullable = false)
  @ColumnDefault("false")
  private boolean isPublic;

  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.REMOVE)
  private List<Repertoire> repertoires = new ArrayList<>();

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

  public List<Repertoire> getRepertoires() {
    return repertoires;
  }

}
