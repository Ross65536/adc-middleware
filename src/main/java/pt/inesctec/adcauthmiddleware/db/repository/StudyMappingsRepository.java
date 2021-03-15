package pt.inesctec.adcauthmiddleware.db.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.StudyMappings;

public interface StudyMappingsRepository extends JpaRepository<StudyMappings, Long> {
    /**
     * Find all Study Mappings for an particular study corresponding to a list scope names
     *
     * @param umaId UMA ID of the study
     * @return Study Mappings
     */
    @Query("SELECT sm.field.name FROM StudyMappings sm WHERE sm.study.umaId = :umaId AND sm.scope.name IN :scopes")
    List<String> findFieldNamesByUmaIdAndScopesIn(@Param("umaId") String umaId, @Param("scopes") List<String> scopes);

    /**
     * Get all Access Scopes for a List of Studies
     *
     * @param umaId UMA ID that identifies this Study
     * @return List of Access Scopes
     */
    @Query("SELECT DISTINCT sm.scope.name FROM StudyMappings sm WHERE sm.study.umaId IN :umaId")
    Set<String> findAccessScopeNamesByUmaIds(@Param("umaId") Set<String> umaIds);
}
