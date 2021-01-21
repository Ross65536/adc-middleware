package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Models the DB's study ID to UMA ID associations.
 */
@Entity
public class AccessScope {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;
}
