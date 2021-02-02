package pt.inesctec.adcauthmiddleware.db;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementModel;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireModel;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.repository.RepertoireRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.models.UmaRegistrationResource;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResourceAttributes;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

/**
 * Responsible for managing, synchronizing the middleware's DB, cache and Keycloak's (or other authorization server) DB.
 */
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

    /**
     * Save resource in the DB.
     *
     * @param repository the repository
     * @param resource   the resource
     * @param <T>        resource type
     * @return true on successful save.
     */
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

    /**
     * Synchronize endpoint entrypoint.
     * Clears out the middleware's Redis cache.
     *
     * @return true on 100% successful synchronization. On false synchronization may have failed for some resources.
     * @throws Exception on internal error.
     */
    @CacheEvict(
            cacheNames = {STUDIES_CACHE_NAME, REPERTOIRES_CACHE_NAME, REARRANGEMENTS_CACHE_NAME},
            allEntries = true)
    public boolean synchronize() throws Exception {
        synchronized (DbRepository.SyncMonitor) {
            return synchronizeGuts();
        }
    }

    /**
     * Get UMA ID given an ADC Study ID. Cached.
     *
     * @param studyId the study ID
     * @return UMA ID. Null if no mapping exists.
     */
    @Cacheable(value = STUDIES_CACHE_NAME, unless = "#result==null")
    public String getStudyUmaId(String studyId) {
        var study = this.studyRepository.findByStudyId(studyId);
        if (study == null) {
            return null;
        }

        return study.getUmaId();
    }

    /**
     * Get UMA ID given an ADC Repertoire ID. Cached.
     *
     * @param repertoireId the repertoire ID
     * @return UMA ID. Null if no mapping exists.
     */
    @Cacheable(value = REPERTOIRES_CACHE_NAME, unless = "#result==null")
    public String getRepertoireUmaId(String repertoireId) {
        var repertoire = this.repertoireRepository.findByRepertoireId(repertoireId);
        if (repertoire == null) {
            return null;
        }

        return repertoire.getStudy().getUmaId();
    }

    /**
     * Get UMA ID given an ADC Rearrangement ID. Cached.
     * In contrast to other getters the rearrangement IDs are not backed in the middleware's DB,
     * if cache misses a request is made to the repository to obtain the rearrangement's repertoire ID, which is stored in the DB.
     *
     * @param rearrangementId the rearrangement ID
     * @return UMA ID. Null if no mapping exists.
     */
    @Cacheable(value = REARRANGEMENTS_CACHE_NAME, unless = "#result==null")
    public String getRearrangementUmaId(String rearrangementId) {
        List<RearrangementModel> rearrangements;

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

    /**
     * Get ADC study ID given an UMA ID. Not cached.
     *
     * @param umaId the UMA ID.
     * @return study ID. Null if no mapping exists.
     */
    public String getUmaStudyId(String umaId) {
        var study = this.studyRepository.findByUmaId(umaId);
        return study == null ? null : study.getStudyId();
    }

    /**
     * Get the set of repertoire IDs given an UMA ID.
     *
     * @param umaId the UMA ID.
     * @return repertoire IDs. null if a mapping in the chain does not exist.
     */
    public Set<String> getUmaRepertoireModel(String umaId) {
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

    /**
     * The synchronize method.
     * Will request repertoires form the repository to obtain the repertoire IDs and their matching study ID and study titles.
     * Will delete the DB associations from repertoire to study used previously.
     * Will create DB associations between the created UMA resource IDs and ADC study IDs, and additionally between the repertoire IDs and study IDs.
     * Will create, update, "delete" resources in Keycloak as needed.
     *
     * @return true on 100% correct synchronization.
     * @throws Exception on internal error.
     */
    @Transactional
    protected boolean synchronizeGuts() throws Exception {
        Logger.info("Synchronizing DB and cache");

        var repertoireSearch = new AdcSearchRequest().addFields(
            AdcConstants.REPERTOIRE_REPERTOIRE_ID_FIELD,
            AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
            AdcConstants.REPERTOIRE_STUDY_TITLE_FIELD
        );

        var repertoires = this.adcClient.getRepertoireModel(repertoireSearch);

        CollectionsUtils.assertList(
            repertoires, e -> e.getRepertoireId() != null,
            "Repertoires response must have a " + AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD
        );

        boolean ok = true;

        // sync studies
        var repositoryStudyMap = CollectionsUtils.toMapKeyByLatest(
                repertoires, RepertoireModel::getStudyId, RepertoireModel::getStudyTitle);

        if (!this.synchronizeStudies(repositoryStudyMap)) {
            ok = false;
        }

        // sync cache
        this.deleteCache();
        if (!this.synchronizeRepertoires(repertoires)) {
            ok = false;
        }

        Logger.info("Finished DB and cache synchronization");

        return ok;
    }

    /**
     * Synchronize repertoire to study DB associations.
     * Some associations might fail, which will be skipped.
     *
     * @param backendRepertoires list of repertoire resources.
     * @return true on 100% successful synchronization.
     */
    protected boolean synchronizeRepertoires(List<RepertoireModel> backendRepertoires) {
        boolean ok = true;

        for (RepertoireModel repertoireIds : backendRepertoires) {
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

    /**
     * Delete DB associations between repertoire IDs and study IDs.
     */
    protected void deleteCache() {
        this.repertoireRepository.deleteAll();
    }

    /**
     * Synchronize the studies state between keycloak, middleware and repository. Will create, update, "delete" UMA resources as needed.
     * UMA resources are not deleted, instead the resource type is set from "study" to "deleted".
     * Will also create the UMA ID to study ID DB associations as needed.
     * Some steps such as deletion or updating might fail, which will be skipped and false returned.
     *
     * @param repositoryStudyMap map with the study IDs coming from the ADC repository and their titles.
     * @return true on 100% successful synchronization.
     * @throws Exception on internal error.
     */
    protected boolean synchronizeStudies(Map<String, String> repositoryStudyMap) throws Exception {
        boolean ok = true;

        var keycloakUmaIds = Set.of(this.umaClient.listUmaResources());
        var repositoryStudyIds = repositoryStudyMap.keySet();
        var dbStudyIds = new HashSet<>(this.loadDbUmaStudyMapping().values());

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
        // 'delete' dangling UMA resources
        for (String danglingUmaId : Sets.difference(keycloakUmaIds, dbUmaIds)) {
            try {
                var resource = this.umaClient.getResource(danglingUmaId);
                var type = resource.getType();
                if (type != null && type.equals(AdcConstants.UMA_DELETED_STUDY_TYPE)) {
                    continue;
                }

                var updateResource = new UmaRegistrationResource();
                updateResource.setName(resource.getName());
                updateResource.setType(AdcConstants.UMA_DELETED_STUDY_TYPE);

                Logger.info("'Deleting' resource {}:{} (updating with: {})", danglingUmaId, resource, updateResource);

                this.umaClient.updateUmaResource(danglingUmaId, updateResource);
            } catch (Exception e) {
                ok = false;
                Logger.error(
                        "Failed to 'delete' UMA resource {}, because: {}", danglingUmaId, e.getMessage());
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
            var studyTitle = repositoryStudyMap.get(newStudyId);
            var umaName = String.format("study ID: %s; title: %s", newStudyId, studyTitle);
            var resource = new UmaRegistrationResource(umaName, AdcConstants.UMA_STUDY_TYPE, allUmaScopes);

            var umaAttributes = new UmaResourceAttributes();
            umaAttributes.setPublicFields(Set.of("repertoire_id"));

            resource.setAttributes(umaAttributes);

            String createdUmaId;
            try {
                createdUmaId = this.umaClient.createUmaResource(resource);
            } catch (Exception e) {
                ok = false;
                Logger.info("Resource {} not created", umaName);
                Logger.debug("Stacktrace: ", e);
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
                updateResource.setType(AdcConstants.UMA_STUDY_TYPE); // keycloak will delete type if not present here

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

    /**
     * Get the DB's UMA ID to study ID mapping.
     *
     * @return DB's UMA ID to study ID mapping
     */
    private Map<String, String> loadDbUmaStudyMapping() {
        return this.studyRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Study::getUmaId, Study::getStudyId));
    }
}
