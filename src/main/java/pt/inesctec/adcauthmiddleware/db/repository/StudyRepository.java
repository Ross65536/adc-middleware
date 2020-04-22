package pt.inesctec.adcauthmiddleware.db.repository;

import org.springframework.data.repository.CrudRepository;
import pt.inesctec.adcauthmiddleware.db.models.Study;

import java.util.List;

public interface StudyRepository extends CrudRepository<Study, Long> {

  List<Study> findByStudyId(String studyId);
}
