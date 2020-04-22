package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.repository.CrudRepository;
import pt.inesctec.adcauthmiddleware.db.models.Rearrangement;

public interface RearrangementRepository extends CrudRepository<Rearrangement, Long> {
}
