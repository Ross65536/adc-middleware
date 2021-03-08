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
     * Find all Study Mappings for an particular study
     *
     * @param umaId UMA ID of the study
     * @return Study Mappings
     */
    @Query("SELECT sm FROM StudyMappings sm WHERE sm.study.umaId = :umaId")
    List<StudyMappings> findByUmaId(@Param("umaId") String umaId);

    /**
     * Get all Access Scopes for a particular Study
     *
     * @param umaId UMA ID that identifies this Study
     * @return List of Access Scopes
     */
    @Query("SELECT DISTINCT(sm.scope) FROM StudyMappings sm WHERE sm.study.umaId = :umaId")
    Set<AccessScope> findAccessScopesByUmaId(@Param("umaId") String umaId);
}
