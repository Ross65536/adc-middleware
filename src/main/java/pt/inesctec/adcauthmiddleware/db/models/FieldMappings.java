package pt.inesctec.adcauthmiddleware.db.models;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Models the DB's study ID to UMA ID associations.
 */
@Entity
public class FieldMappings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
