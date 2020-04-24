package pt.inesctec.adcauthmiddleware.db;

import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.repository.RearrangementRepository;
import pt.inesctec.adcauthmiddleware.db.repository.RepertoireRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;

public class CacheRepository {
  private final AdcClient adcClient;
  private final StudyRepository studyRepository;
  private final RepertoireRepository repertoireRepository;
  private final RearrangementRepository rearrangementRepository;

  public CacheRepository(AdcClient adcClient, StudyRepository studyRepository, RepertoireRepository repertoireRepository, RearrangementRepository rearrangementRepository) {
    this.adcClient = adcClient;
    this.studyRepository = studyRepository;
    this.repertoireRepository = repertoireRepository;
    this.rearrangementRepository = rearrangementRepository;
  }

  public void synchronize() {
    this.studyRepository.save(new Study("a2", "bb"));

  }
}
