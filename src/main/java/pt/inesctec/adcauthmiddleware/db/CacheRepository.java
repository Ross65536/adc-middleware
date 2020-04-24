package pt.inesctec.adcauthmiddleware.db;

import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.db.repository.RearrangementRepository;
import pt.inesctec.adcauthmiddleware.db.repository.RepertoireRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CacheRepository {
  private final AdcClient adcClient;
  private final UmaClient umaClient;
  private final StudyRepository studyRepository;
  private final RepertoireRepository repertoireRepository;
  private final RearrangementRepository rearrangementRepository;

  public CacheRepository(AdcClient adcClient, UmaClient umaClient, StudyRepository studyRepository, RepertoireRepository repertoireRepository, RearrangementRepository rearrangementRepository) {
    this.adcClient = adcClient;
    this.umaClient = umaClient;
    this.studyRepository = studyRepository;
    this.repertoireRepository = repertoireRepository;
    this.rearrangementRepository = rearrangementRepository;
  }

  public void synchronize() throws Exception {
    final var idsRequest = AdcSearchRequest.buildIdsStudySearch();
    var backendRepertoires = this.adcClient.getRepertoireIds(idsRequest);
    var umaResources = this.umaClient.listUmaResources();
    var dbStudies = this.studyRepository.findAll();

    var dbUmaIds = StreamSupport.stream(dbStudies.spliterator(), false)
        .map(e -> e.getUmaId())
        .collect(Collectors.toSet());

    var danglingUmaIds = Arrays.stream(umaResources)
        .filter(uma -> ! dbUmaIds.contains(uma))
        .collect(Collectors.toList());

    for (String danglingUma : danglingUmaIds) {
      this.umaClient.deleteUmaResource(danglingUma);
    }

  }
}
