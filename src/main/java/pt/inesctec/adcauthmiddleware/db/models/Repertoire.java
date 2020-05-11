package pt.inesctec.adcauthmiddleware.db.models;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class Repertoire {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(unique = true, nullable = false)
  private String repertoireId;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Study study;

  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.REMOVE)
  private List<Rearrangement> rearrangements = new ArrayList<>();

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

  public List<Rearrangement> getRearrangements() {
    return rearrangements;
  }
}
