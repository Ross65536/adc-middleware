package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

public interface AdcFieldsRepository extends JpaRepository<AdcFields, Long> {
    AdcFields findByName(String name);
}
