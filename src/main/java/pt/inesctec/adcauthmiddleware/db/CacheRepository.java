package pt.inesctec.adcauthmiddleware.db;

import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;

public class CacheRepository {
  private final AdcClient adcClient;
  private StudyRepository studyRepository;

  public CacheRepository(AdcClient adcClient, StudyRepository studyRepository) {
    this.adcClient = adcClient;
    this.studyRepository = studyRepository;
  }

  public void synchronize() {
    this.studyRepository.save(new Study("1", "2"));

  }
}
