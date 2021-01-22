package pt.inesctec.adcauthmiddleware.db.models;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Models the DB's repertoire ID to study ID associations.
 */
@Entity
public class Repertoire {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String repertoireId;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Study study;

  public Repertoire() {}

  public Repertoire(String repertoireId, Study study) {
    this.repertoireId = repertoireId;
    this.study = study;
  }

  public String getRepertoireId() {
    return repertoireId;
  }

  public Study getStudy() {
    return study;
  }

  @Override
  public String toString() {
    return String.format("{repertoireId: %s}", repertoireId);
  }
}
