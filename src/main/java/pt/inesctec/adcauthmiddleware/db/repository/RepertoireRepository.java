package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;
import pt.inesctec.adcauthmiddleware.db.models.Study;

import java.util.List;

public interface RepertoireRepository extends JpaRepository<Repertoire, Long> {
  Repertoire findByRepertoireId(String repertoireId);

  List<Repertoire> findByStudy(Study umaId);
}
