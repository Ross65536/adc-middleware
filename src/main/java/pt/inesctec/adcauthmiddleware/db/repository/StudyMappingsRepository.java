package pt.inesctec.adcauthmiddleware.db.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.StudyMappings;
import pt.inesctec.adcauthmiddleware.db.models.Templates;

public interface StudyMappingsRepository extends JpaRepository<StudyMappings, Long> {
    @Query("SELECT sm FROM StudyMappings sm WHERE sm.study.umaId = :umaId")
    StudyMappings findByUmaId(@Param("umaId") String umaId);

    @Query("SELECT sm.scope FROM StudyMappings sm WHERE sm.study.umaId = :umaId")
    List<AccessScope> findAccessScopesByUmaId(@Param("umaId") String umaId);
}
