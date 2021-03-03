package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.inesctec.adcauthmiddleware.db.models.Templates;

public interface TemplatesRepository extends JpaRepository<Templates, Long> {
    @Query("SELECT tp FROM Templates tp WHERE id = (SELECT td.id FROM TemplateDefault td)")
    Templates findDefault();
}
