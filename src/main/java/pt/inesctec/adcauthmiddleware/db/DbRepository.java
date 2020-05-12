package pt.inesctec.adcauthmiddleware.db;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementIds;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireIds;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.repository.RepertoireRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.models.UmaRegistrationResource;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

@Component
public class DbRepository {
  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(DbRepository.class);
  private static final Object SyncMonitor = new Object();
  private static final String STUDIES_CACHE_NAME = "studies";
  private static final String REPERTOIRES_CACHE_NAME = "repertoires";
  private static final String REARRANGEMENTS_CACHE_NAME = "rearrangements";

  private final AdcClient adcClient;
  private final UmaClient umaClient;
  private final StudyRepository studyRepository;
  private final RepertoireRepository repertoireRepository;
  private final CsvConfig csvConfig;

  public DbRepository(
      AdcClient adcClient,
      UmaClient umaClient,
      StudyRepository studyRepository,
      RepertoireRepository repertoireRepository,
      CsvConfig csvConfig) {
    this.adcClient = adcClient;
    this.umaClient = umaClient;
    this.studyRepository = studyRepository;
    this.repertoireRepository = repertoireRepository;
    this.csvConfig = csvConfig;
  }

  @CacheEvict(
      cacheNames = {STUDIES_CACHE_NAME, REPERTOIRES_CACHE_NAME, REARRANGEMENTS_CACHE_NAME},
      allEntries = true)
  public void synchronize() throws Exception {
    synchronized (DbRepository.SyncMonitor) {
      synchronizeGuts();
    }
  }

  @Cacheable(value = STUDIES_CACHE_NAME, unless = "#result==null")
  public String getStudyUmaId(String studyId) {
    var study = this.studyRepository.findByStudyId(studyId);
    if (study == null) {
      return null;
    }

    return study.getUmaId();
  }

  @Cacheable(value = REPERTOIRES_CACHE_NAME, unless = "#result==null")
  public String getRepertoireUmaId(String repertoireId) {
    var repertoire = this.repertoireRepository.findByRepertoireId(repertoireId);
    if (repertoire == null) {
      return null;
    }

    return repertoire.getStudy().getUmaId();
  }

  @Cacheable(value = REARRANGEMENTS_CACHE_NAME, unless = "#result==null")
  public String getRearrangementUmaId(String rearrangementId) {
    List<RearrangementIds> rearrangements;

    try {
      rearrangements = this.adcClient.getRearrangement(rearrangementId);
    } catch (Exception e) {
      Logger.error(
          String.format(
              "Cache: Can't get rearrangement's '%s' UMA ID because: %s",
              rearrangementId, e.getMessage()));
      Logger.debug("Stacktrace: ", e);
      return null;
    }

    if (rearrangements.size() != 1) { // not found
      return null;
    }

    var repertoireId = rearrangements.get(0).getRepertoireId();
    if (repertoireId == null) {
      Logger.error("Response's rearrangement can't have a null rearrangement_id");
      return null;
    }

    return this.getRepertoireUmaId(repertoireId);
  }

  public String getUmaStudyId(String umaId) {
    var study = this.studyRepository.findByUmaId(umaId);
    return study == null ? null : study.getStudyId();
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

    return repertoires.stream().map(Repertoire::getRepertoireId).collect(Collectors.toSet());
  }

  @Transactional
  protected void synchronizeGuts() throws Exception {
    Logger.info("Synchronizing DB and cache");

    var repertoireSearch =
        new AdcSearchRequest()
            .addFields(
                AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD,
                AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
                AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD);
    var backendRepertoires = this.adcClient.getRepertoireIds(repertoireSearch);
    CollectionsUtils.assertList(
        backendRepertoires,
        e -> e.getRepertoireId() != null,
        "Repertoires response must have repertoire_id");

    var rearrangementSearch =
        new AdcSearchRequest()
            .addFields(
                AdcConstants.REARRANGEMENT_REARRANGEMENT_ID_FIELD,
                AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);
    var backendRearrangements = this.adcClient.getRearrangementIds(rearrangementSearch);
    CollectionsUtils.assertList(
        backendRearrangements,
        e -> e.getRearrangementId() != null,
        "Rearrangements response must have rearrangement_id");

    // sync studies
    var backendStudyMap =
        CollectionsUtils.toMapKeyByLatest(
            backendRepertoires, RepertoireIds::getStudyId, RepertoireIds::getStudyTitle);
    this.synchronizeStudies(backendStudyMap);

    // sync cache
    this.deleteCache();
    this.synchronizeRepertoires(backendRepertoires);

    Logger.info("Finished DB and cache synchronization");
  }

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

  protected void deleteCache() {
    this.repertoireRepository.deleteAll();
  }

  protected void synchronizeStudies(Map<String, String> backendStudyMap) throws Exception {
    var umaResources = List.of(this.umaClient.listUmaResources());
    var dbStudies = this.studyRepository.findAll();

    var serverUmaIds = new HashSet<>(umaResources);
    var dbUmaIds = dbStudies.stream().map(Study::getUmaId).collect(Collectors.toSet());

    // delete dangling UMA resources
    Sets.difference(serverUmaIds, dbUmaIds)
        .forEach(
            danglingUma -> {
              try {
                this.umaClient.deleteUmaResource(danglingUma);
              } catch (Exception e) {
                Logger.error(
                    "Failed to delete UMA resource {}, because: {}", danglingUma, e.getMessage());
                Logger.debug("Stacktrace: ", e);
              }
            });

    // delete dangling DB resources
    Sets.difference(dbUmaIds, serverUmaIds)
        .forEach(
            danglingDbUmaId -> {
              Logger.info("Deleting DB study with uma ID: {}", danglingDbUmaId);
              try {
                this.studyRepository.deleteByUmaId(danglingDbUmaId);
              } catch (RuntimeException e) {
                Logger.error(
                    "Failed to delete DB study with UMA ID {}, because: {}",
                    danglingDbUmaId,
                    e.getMessage());
                Logger.debug("Stacktrace: ", e);
              }
            });

    // add new resources
    var allUmaScopes = this.csvConfig.getAllUmaScopes();
    dbStudies = this.studyRepository.findAll();
    var backendStudySet = backendStudyMap.keySet();
    var dbStudyIds = dbStudies.stream().map(Study::getStudyId).collect(Collectors.toSet());
    Sets.difference(backendStudySet, dbStudyIds)
        .forEach(
            newStudyId -> {
              var studyTitle = backendStudyMap.get(newStudyId);
              var umaName = String.format("study ID: %s; title: %s", newStudyId, studyTitle);
              var newUmaResource =
                  new UmaRegistrationResource(umaName, AdcConstants.UMA_STUDY_TYPE, allUmaScopes);

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
                this.studyRepository.saveAndFlush(study);
              } catch (RuntimeException e) {
                Logger.error("Failed to create DB study {} because: {}", study, e.getMessage());
                Logger.debug("Stacktrace: ", e);
              }
            });

    // validate common resources
    Sets.intersection(serverUmaIds, dbUmaIds)
        .forEach(
            umaId -> {
              try {
                var resource = this.umaClient.getResource(umaId);
                Set<String> actualResources = resource.getResourceScopes();
                if (actualResources.containsAll(allUmaScopes)) {
                  return;
                }

                // update resource if scopes are not matching

                var updateResources = Sets.union(actualResources, allUmaScopes);
                var updateResource = new UmaRegistrationResource();
                updateResource.setName(resource.getName()); // mandatory by keycloak
                updateResource.setResourceScopes(updateResources);
                updateResource.setType(
                    AdcConstants.UMA_STUDY_TYPE); // keycloak will delete type if not present here

                Logger.info("Updating resource {}:{} with {}", umaId, resource, updateResource);

                this.umaClient.updateUmaResource(umaId, updateResource);
              } catch (Exception e) {
                Logger.error("Failed to check UMA resource {}, because: {}", umaId, e.getMessage());
                Logger.debug("Stacktrace: ", e);
              }
            });
  }

  public static <T> void saveResource(CrudRepository<T, ?> repository, T resource) {
    Logger.debug("Saving resource {}", resource);

    try {
      repository.save(resource);
    } catch (RuntimeException e) {
      Logger.error("Failed to save cache resource {}, because: {}", resource, e.getMessage());
      Logger.debug("Stacktrace: ", e);
    }
  }
}
