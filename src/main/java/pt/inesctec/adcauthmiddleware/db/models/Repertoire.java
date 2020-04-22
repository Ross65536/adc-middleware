package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
public class Repertoire {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(unique = true, nullable = false)
  private String repertoireId;

  @ManyToOne
  @NotNull
  private Study study;

  public String getRepertoireId() {
    return repertoireId;
  }

  public Study getStudy() {
    return study;
  }
}
