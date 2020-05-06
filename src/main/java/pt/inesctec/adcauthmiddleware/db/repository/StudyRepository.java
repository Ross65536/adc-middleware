package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.repository.CrudRepository;
import pt.inesctec.adcauthmiddleware.db.models.Study;

import java.util.List;


public interface StudyRepository extends CrudRepository<Study, Long> {

  Study findByStudyId(String studyId);

  Study findByUmaId(String umaId);

  void deleteByUmaId(String umaId);
}
