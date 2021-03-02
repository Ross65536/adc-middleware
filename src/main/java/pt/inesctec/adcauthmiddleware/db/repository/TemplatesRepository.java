package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.inesctec.adcauthmiddleware.db.models.Templates;

public interface TemplatesRepository extends JpaRepository<Templates, Long> {}
