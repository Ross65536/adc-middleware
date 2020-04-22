package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
public class Rearrangement {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(unique = true, nullable = false)
  private String rearrangementId;

  @ManyToOne
  @NotNull
  private Repertoire repertoire;

  public String getRearrangementId() {
    return rearrangementId;
  }

  public Repertoire getRepertoire() {
    return repertoire;
  }
}
