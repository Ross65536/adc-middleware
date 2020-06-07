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
  public boolean synchronize() throws Exception {
    synchronized (DbRepository.SyncMonitor) {
      return synchronizeGuts();
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
      Logger.error("Response's rearrangement can't have a null " + AdcConstants.REARRANGEMENT_REARRANGEMENT_ID_FIELD);
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
  protected boolean synchronizeGuts() throws Exception {
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
        "Repertoires response must have a " + AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);

    boolean ok = true;
    // sync studies
    var backendStudyMap =
        CollectionsUtils.toMapKeyByLatest(
            backendRepertoires, RepertoireIds::getStudyId, RepertoireIds::getStudyTitle);
    if (!this.synchronizeStudies(backendStudyMap)) {
      ok = false;
    }

    // sync cache
    this.deleteCache();
    if (!this.synchronizeRepertoires(backendRepertoires)) {
      ok = false;
    }

    Logger.info("Finished DB and cache synchronization");

    return ok;
  }

  protected boolean synchronizeRepertoires(List<RepertoireIds> backendRepertoires) {
    boolean ok = true;

    for (RepertoireIds repertoireIds : backendRepertoires) {
      var studyId = repertoireIds.getStudyId();
      var study = this.studyRepository.findByStudyId(studyId);
      if (study == null) {
        Logger.error("Invalid DB state, missing study {}, skipping", studyId);
        continue;
      }

      var repertoire = new Repertoire(repertoireIds.getRepertoireId(), study);
      if (!DbRepository.saveResource(this.repertoireRepository, repertoire)) {
        ok = false;
      }
    }

    return ok;
  }

  protected void deleteCache() {
    this.repertoireRepository.deleteAll();
  }

  protected boolean synchronizeStudies(Map<String, String> backendStudyMap) throws Exception {
    boolean ok = true;

    var keycloakUmaIds = Set.of(this.umaClient.listUmaResources());
    var dbStudyIds = new HashSet<>(loadDbUmaStudyMapping().values());
    var repositoryStudyIds = backendStudyMap.keySet();

    // delete dangling DB resources
    for (String danglingDbStudyId : Sets.difference(dbStudyIds, repositoryStudyIds)) {
      Logger.info("Deleting DB study with study ID: {}", danglingDbStudyId);
      try {
        this.studyRepository.deleteByStudyId(danglingDbStudyId);
      } catch (RuntimeException e) {
        ok = false;
        Logger.error(
            "Failed to delete DB study with study ID {}, because: {}",
            danglingDbStudyId,
            e.getMessage());
        Logger.debug("Stacktrace: ", e);
      }
    }
    this.studyRepository.flush();

    var dbUmaIds = loadDbUmaStudyMapping().keySet();
    // delete dangling UMA resources
    for (String danglingUma : Sets.difference(keycloakUmaIds, dbUmaIds)) {
      try {
        this.umaClient.deleteUmaResource(danglingUma);
      } catch (Exception e) {
        ok = false;
        Logger.error(
            "Failed to delete UMA resource {}, because: {}", danglingUma, e.getMessage());
        Logger.debug("Stacktrace: ", e);
      }
    }

    // delete dangling DB resources
    for (String danglingDbUmaId : Sets.difference(dbUmaIds, keycloakUmaIds)) {
      Logger.info("Deleting DB study with uma ID: {}", danglingDbUmaId);
      try {
        this.studyRepository.deleteByUmaId(danglingDbUmaId);
      } catch (RuntimeException e) {
        ok = false;
        Logger.error(
            "Failed to delete DB study with UMA ID {}, because: {}",
            danglingDbUmaId,
            e.getMessage());
        Logger.debug("Stacktrace: ", e);
      }
    }

    // add new resources
    var allUmaScopes = this.csvConfig.getAllUmaScopes();
    dbStudyIds = new HashSet<>(loadDbUmaStudyMapping().values());
    for (String newStudyId : Sets.difference(repositoryStudyIds, dbStudyIds)) {
      var studyTitle = backendStudyMap.get(newStudyId);
      var umaName = String.format("study ID: %s; title: %s", newStudyId, studyTitle);
      var newUmaResource =
          new UmaRegistrationResource(umaName, AdcConstants.UMA_STUDY_TYPE, allUmaScopes);

      String createdUmaId = null;
      try {
        createdUmaId = this.umaClient.createUmaResource(newUmaResource);
      } catch (Exception e) {
        ok = false;
        Logger.info("Resource {} not created", umaName);
        Logger.info("Stacktrace: ", e);
        continue;
      }

      var study = new Study(newStudyId, createdUmaId);
      Logger.info("Creating DB study {}", study);
      try {
        this.studyRepository.saveAndFlush(study);
      } catch (RuntimeException e) {
        ok = false;
        Logger.error("Failed to create DB study {} because: {}", study, e.getMessage());
        Logger.debug("Stacktrace: ", e);
      }
    }

    // validate common resources
    for (String umaId : Sets.intersection(keycloakUmaIds, dbUmaIds)) {
      try {
        var resource = this.umaClient.getResource(umaId);
        Set<String> actualResources = resource.getResourceScopes();
        if (actualResources.containsAll(allUmaScopes)) {
          continue;
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
        ok = false;
        Logger.error("Failed to check UMA resource {}, because: {}", umaId, e.getMessage());
        Logger.debug("Stacktrace: ", e);
      }
    }

    return ok;
  }

  private Map<String, String> loadDbUmaStudyMapping() {
    return this.studyRepository.findAll()
        .stream()
        .collect(Collectors.toMap(Study::getUmaId, Study::getStudyId));
  }

  public static <T> boolean saveResource(CrudRepository<T, ?> repository, T resource) {
    Logger.debug("Saving resource {}", resource);

    try {
      repository.save(resource);
    } catch (RuntimeException e) {
      Logger.error("Failed to save cache resource {}, because: {}", resource, e.getMessage());
      Logger.debug("Stacktrace: ", e);
      return false;
    }

    return true;
  }
}
