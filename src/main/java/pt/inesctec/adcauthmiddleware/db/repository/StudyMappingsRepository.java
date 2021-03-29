package pt.inesctec.adcauthmiddleware.db.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.inesctec.adcauthmiddleware.db.models.AdcFieldType;
import pt.inesctec.adcauthmiddleware.db.models.StudyMappings;

public interface StudyMappingsRepository extends JpaRepository<StudyMappings, Long> {
    /**
     * Get all Access Scopes for a list of studies and for an ADC field type.
     *
     * @param umaIds Set of UMA IDs that identify the studies
     * @return Set of Access Scopes
     */
    @Query(
        "SELECT DISTINCT sm.scope.name FROM StudyMappings sm "
        + "WHERE sm.study.umaId IN :umaId "
        + "AND sm.field.type = :adcFieldType"
    )
    Set<String> findScopesByUmaIds(
        @Param("umaId") Set<String> umaIds,
        @Param("adcFieldType") AdcFieldType adcFieldType
    );

    /**
     * Find all Access Scopes for a list of studies and for a specific list of ADC Field names.
     *
     * @param umaIds Set of UMA IDs that identify the studies
     * @param adcFields Set of ADC Fields to filter the scopes for
     * @return Set of Access Scopes
     */
    @Query(
        "SELECT DISTINCT sm.scope.name FROM StudyMappings sm "
        + "WHERE sm.study.umaId IN :umaId "
        + "AND sm.field.type = :adcFieldType "
        + "AND sm.field.name IN :adcFields"
    )
    Set<String> findScopesByUmaIdsAndByFields(
        @Param("umaId") Set<String> umaIds,
        @Param("adcFieldType") AdcFieldType adcFieldType,
        @Param("adcFields") Set<String> adcFields
    );

    /**
     * Find all Study Mappings for an particular study corresponding to a list scope names.
     *
     * @param umaId UMA ID of the study
     * @return Study Mappings
     */
    @Query(
        "SELECT sm.field.name FROM StudyMappings sm "
        + "WHERE sm.study.umaId = :umaId "
        + "AND sm.field.type = :adcFieldType "
        + "AND sm.scope.name IN :scopes"
    )
    List<String> findMappings(
        @Param("umaId") String umaId,
        @Param("adcFieldType") AdcFieldType adcFieldType,
        @Param("scopes") List<String> scopes);
}
