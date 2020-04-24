package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.repository.CrudRepository;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;

public interface RepertoireRepository extends CrudRepository<Repertoire, Long> {
  Repertoire findByRepertoireId(String repertoireId);
}
