package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import pt.inesctec.adcauthmiddleware.db.models.Study;

import javax.transaction.Transactional;


public interface StudyRepository extends JpaRepository<Study, Long> {

  Study findByStudyId(String studyId);

  Study findByUmaId(String umaId);

  @Transactional
  @Modifying
  void deleteByUmaId(String umaId);
}
