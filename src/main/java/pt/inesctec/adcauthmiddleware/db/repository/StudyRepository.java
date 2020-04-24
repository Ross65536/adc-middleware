package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.repository.CrudRepository;
import pt.inesctec.adcauthmiddleware.db.models.Study;


public interface StudyRepository extends CrudRepository<Study, Long> {

  Study findByStudyId(String studyId);

  void deleteByUmaId(String umaId);
}
