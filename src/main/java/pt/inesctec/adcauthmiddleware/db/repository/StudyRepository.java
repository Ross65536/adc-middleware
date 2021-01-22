package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import pt.inesctec.adcauthmiddleware.db.models.Study;

import javax.transaction.Transactional;

/**
 * DB repository for study operations.
 */
public interface StudyRepository extends JpaRepository<Study, Long> {

  Study findByStudyId(String studyId);

  Study findByUmaId(String umaId);

  @Transactional
  @Modifying
  void deleteByUmaId(String umaId);

  @Transactional
  @Modifying
  Long deleteByStudyId(String studyId);
}
