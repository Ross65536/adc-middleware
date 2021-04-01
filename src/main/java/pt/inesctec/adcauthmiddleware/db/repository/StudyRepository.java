package pt.inesctec.adcauthmiddleware.db.repository;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import pt.inesctec.adcauthmiddleware.db.models.Study;

/**
 * DB repository for study operations.
 */
public interface StudyRepository extends JpaRepository<Study, Long> {
    /**
     * Get all study database IDs mapped by UMA ID.
     *
     * @return Study ID mapped by UMA ID [UMA ID] -> Database ID
     */
    default Map<String, String> findAllMapByUmaId() {
        return findAll().stream().collect(Collectors.toMap(Study::getUmaId, Study::getStudyId));
    }

    Study findByStudyId(String studyId);

    Study findByUmaId(String umaId);

    @Transactional
    @Modifying
    void deleteByUmaId(String umaId);

    @Transactional
    @Modifying
    void deleteByStudyId(String studyId);
}
