package pt.inesctec.adcauthmiddleware.db;

import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementIds;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireIds;
import pt.inesctec.adcauthmiddleware.db.models.Rearrangement;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class DbRepository {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(DbRepository.class);
  private static Object SyncMonitor = new Object();

  private final AdcClient adcClient;
  private final UmaClient umaClient;
  private final StudyRepository studyRepository;
  private final RepertoireRepository repertoireRepository;
  private final RearrangementRepository rearrangementRepository;

  public DbRepository(
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

  @CacheEvict(
      cacheNames = {"studies", "repertoires", "rearrangements"},
      allEntries = true)
  public void synchronize() throws Exception {
    synchronized (DbRepository.SyncMonitor) {
      synchronizeGuts();
    }
  }

  @Cacheable("studies")
  public String getStudyUmaId(String studyId) {
    var study = this.studyRepository.findByStudyId(studyId);
    if (study == null) {
      return null;
    }

    return study.getUmaId();
  }

  @Cacheable("repertoires")
  public String getRepertoireUmaId(String repertoireId) {
    var repertoire = this.repertoireRepository.findByRepertoireId(repertoireId);
    if (repertoire == null) {
      return null;
    }

    return repertoire.getStudy().getUmaId();
  }

  @Cacheable("rearrangements")
  public String getRearrangementUmaId(String rearrangementId) {
    var rearrangement = this.rearrangementRepository.findByRearrangementId(rearrangementId);
    if (rearrangement == null) {
      return null;
    }

    return rearrangement.getRepertoire().getStudy().getUmaId();
  }

  public String getUmaStudyId(String umaId) {
    var study = this.studyRepository.findByUmaId(umaId);
    return study == null ? null :  study.getStudyId();
  }

  public Set<String> getUmaRepertoireIds(String umaId) {
    var study = this.studyRepository.findByUmaId(umaId);
    if (study == null) {
      return null;
    }

    var repertoires = this.repertoireRepository.findByStudy(study);
    if (repertoires == null) {
      return null;
    }

    return repertoires.stream()
        .map(Repertoire::getRepertoireId)
        .collect(Collectors.toSet());
  }

  private void synchronizeGuts() throws Exception {
    Logger.info("Synchronizing DB and cache");

    var repertoireSearch =
        new AdcSearchRequest()
            .addFields(
                AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD,
                AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
                AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD);
    var backendRepertoires = this.adcClient.getRepertoireIds(repertoireSearch);
    CollectionsUtils.assertList(backendRepertoires, e -> e.getRepertoireId() != null, "Repertoires response must have repertoire_id");

    var rearrangementSearch =
        new AdcSearchRequest()
            .addFields(
                AdcConstants.REARRANGEMENT_REARRANGEMENT_ID_FIELD,
                AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);
    var backendRearrangements = this.adcClient.getRearrangementIds(rearrangementSearch);
    CollectionsUtils.assertList(backendRearrangements, e -> e.getRearrangementId() != null, "Rearrangements response must have rearrangement_id");

    // sync studies
    var backendStudyMap =
        CollectionsUtils.toMapKeyByLatest(
            backendRepertoires, RepertoireIds::getStudyId, RepertoireIds::getStudyTitle);
    this.synchronizeStudies(backendStudyMap);

    // sync cache
    this.deleteCache();
    this.synchronizeRepertoires(backendRepertoires);
    this.synchronizeRearrangements(backendRearrangements);

    Logger.info("Finished DB and cache synchronization");
  }

  @Transactional
  protected void synchronizeRepertoires(List<RepertoireIds> backendRepertoires) {
    backendRepertoires.forEach(
        repertoireIds -> {
          var studyId = repertoireIds.getStudyId();
          var study = this.studyRepository.findByStudyId(studyId);
          if (study == null) {
            Logger.error("Invalid DB state, missing study {}, skipping", studyId);
            return;
          }

          var repertoire = new Repertoire(repertoireIds.getRepertoireId(), study);
          DbRepository.saveResource(this.repertoireRepository, repertoire);
        });
  }

  @Transactional
  protected void synchronizeRearrangements(List<RearrangementIds> backendRepertoires) {
    backendRepertoires.forEach(
        repertoireIds -> {
          var repertoireId = repertoireIds.getRepertoireId();
          var repertoire = this.repertoireRepository.findByRepertoireId(repertoireId);
          if (repertoire == null) {
            Logger.error("Invalid DB state, missing repertoire {}, skipping", repertoireId);
            return;
          }

          var rearrangement = new Rearrangement(repertoireIds.getRearrangementId(), repertoire);
          DbRepository.saveResource(this.rearrangementRepository, rearrangement);
        });
  }

  @Transactional
  protected void deleteCache() {
    this.rearrangementRepository.deleteAll();
    this.repertoireRepository.deleteAll();
  }

  @Transactional
  protected void synchronizeStudies(Map<String, String> backendStudyMap) throws Exception {
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
                      umaName, AdcConstants.UMA_STUDY_TYPE, AdcConstants.AllUmaScopes);

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

  public static <T> void saveResource(CrudRepository<T, ?> repository, T resource) {
    Logger.debug("Saving resource {}", resource);

    try {
      repository.save(resource);
    } catch (RuntimeException e) {
      Logger.error("Failed to save cache resource {}, because: {}", resource, e);
      Logger.debug("Stacktrace: ", e);
    }
  }
}
