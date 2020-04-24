package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.*;
import java.util.List;

@Entity
public class Study {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(unique = true, nullable = false)
  private String studyId;

  @Column(unique = true, nullable = false)
  private String umaId;

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
