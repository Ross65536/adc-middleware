package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.inesctec.adcauthmiddleware.db.models.AdcFieldType;

public interface AdcFieldTypeRepository extends JpaRepository<AdcFieldType, Long> {
    AdcFieldType findByName(String name);
}
