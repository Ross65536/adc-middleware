package pt.inesctec.adcauthmiddleware.db.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.AdcFieldType;

public interface AdcFieldTypeRepository extends JpaRepository<AdcFieldType, Long> {
    AdcFieldType findByName(String name);
}
