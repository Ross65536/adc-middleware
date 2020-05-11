package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.inesctec.adcauthmiddleware.db.models.Rearrangement;

public interface RearrangementRepository extends JpaRepository<Rearrangement, Long> {
  Rearrangement findByRearrangementId(String rearrangementId);
}
