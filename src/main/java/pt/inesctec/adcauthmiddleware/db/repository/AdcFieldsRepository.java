package pt.inesctec.adcauthmiddleware.db.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.inesctec.adcauthmiddleware.db.models.AdcFieldType;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

public interface AdcFieldsRepository extends JpaRepository<AdcFields, Long> {
    AdcFields findByName(String name);

    List<AdcFields> findByType(AdcFieldType type);
}
