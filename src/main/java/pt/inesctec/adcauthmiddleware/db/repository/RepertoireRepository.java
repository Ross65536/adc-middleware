package pt.inesctec.adcauthmiddleware.db.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;
import pt.inesctec.adcauthmiddleware.db.models.Study;

public interface RepertoireRepository extends JpaRepository<Repertoire, Long> {
  Repertoire findByRepertoireId(String repertoireId);

  List<Repertoire> findByStudy(Study umaId);
}
