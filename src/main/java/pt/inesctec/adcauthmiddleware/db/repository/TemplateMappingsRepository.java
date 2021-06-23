package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.inesctec.adcauthmiddleware.db.models.AdcFieldType;
import pt.inesctec.adcauthmiddleware.db.models.StudyMappings;
import pt.inesctec.adcauthmiddleware.db.models.TemplateMappings;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

public interface TemplateMappingsRepository extends JpaRepository<TemplateMappings, Long> {
    @Transactional
    @Modifying
    @Query(
        value = "INSERT INTO template_mappings "
        + "(id_template, id_adc_field, id_access_scope) "
        + "VALUES (:idTemplate, :idAdcFields, :idAccessScope)",
        nativeQuery = true
    )
    void saveMappings(
            @Param("idTemplate") long idTemplate,
            @Param("idAdcFields") int idAdcFields,
            @Param("idAccessScope") long idAccessScope
    );
}
