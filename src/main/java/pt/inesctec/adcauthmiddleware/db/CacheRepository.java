package pt.inesctec.adcauthmiddleware.db;

import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcUtils;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireIds;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.repository.RearrangementRepository;
import pt.inesctec.adcauthmiddleware.db.repository.RepertoireRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.models.UmaRegistrationResource;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class CacheRepository {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(CacheRepository.class);

  private final AdcClient adcClient;
  private final UmaClient umaClient;
  private final StudyRepository studyRepository;
  private final RepertoireRepository repertoireRepository;
  private final RearrangementRepository rearrangementRepository;

  public CacheRepository(
      AdcClient adcClient,
      UmaClient umaClient,
      StudyRepository studyRepository,
      RepertoireRepository repertoireRepository,
      RearrangementRepository rearrangementRepository) {
    this.adcClient = adcClient;
    this.umaClient = umaClient;
    this.studyRepository = studyRepository;
    this.repertoireRepository = repertoireRepository;
    this.rearrangementRepository = rearrangementRepository;
  }

  @Transactional
  public void synchronize() throws Exception {
    final var idsRequest = AdcSearchRequest.buildIdsStudySearch();
    var backendRepertoires = this.adcClient.getRepertoireIds(idsRequest);
    var backendStudyMap =
        CollectionsUtils.toMapKeyByLatest(
            backendRepertoires, RepertoireIds::getStudyId, RepertoireIds::getStudyTitle);
    this.synchronizeStudies(backendStudyMap);
  }

  @Transactional
  public void synchronizeStudies(Map<String, String> backendStudyMap) throws Exception {
    var umaResources = List.of(this.umaClient.listUmaResources());
    var dbStudies = this.studyRepository.findAll();

    var umaIdSet = new HashSet<>(umaResources);
    var dbUmaIds =
        StreamSupport.stream(dbStudies.spliterator(), false)
            .map(Study::getUmaId)
            .collect(Collectors.toSet());

    // delete dangling UMA resources
    Sets.difference(umaIdSet, dbUmaIds)
        .forEach(
            danglingUma -> {
              try {
                this.umaClient.deleteUmaResource(danglingUma);
              } catch (Exception e) {
                Logger.error("Failed to delete UMA resource {}, because: {}", danglingUma, e);
                Logger.debug("Stacktrace: ", e);
              }
            });

    // delete dangling DB resources
    Sets.difference(dbUmaIds, umaIdSet)
        .forEach(
            danglingDbUmaId -> {
              Logger.info("Deleting DB study with uma ID: {}", danglingDbUmaId);
              try {
                this.studyRepository.deleteByUmaId(danglingDbUmaId);
              } catch (RuntimeException e) {
                Logger.error(
                    "Failed to delete DB study with UMA ID {}, because: {}", danglingDbUmaId, e);
                Logger.debug("Stacktrace: ", e);
              }
            });

    // add new resources
    var backendStudySet = backendStudyMap.keySet();
    var dbStudyIds =
        StreamSupport.stream(dbStudies.spliterator(), false)
            .map(Study::getStudyId)
            .collect(Collectors.toSet());
    Sets.difference(backendStudySet, dbStudyIds)
        .forEach(
            newStudyId -> {
              var studyTitle = backendStudyMap.get(newStudyId);
              var umaName = String.format("study ID: %s; title: %s", newStudyId, studyTitle);
              var newUmaResource =
                  new UmaRegistrationResource(
                      umaName, AdcUtils.UMA_STUDY_TYPE, AdcUtils.AllUmaScopes);

              String createdUmaId = null;
              try {
                createdUmaId = this.umaClient.createUmaResource(newUmaResource);
              } catch (Exception e) {
                Logger.info("Resource {} not created", umaName);
                Logger.info("Stacktrace: ", e);
                return;
              }

              var study = new Study(newStudyId, createdUmaId);
              Logger.info("Creating DB study {}", study);
              try {
                this.studyRepository.save(study);
              } catch (RuntimeException e) {
                Logger.error("Failed to create DB study {} because: {}", study, e);
                Logger.debug("Stacktrace: ", e);
              }
            });
  }
}
