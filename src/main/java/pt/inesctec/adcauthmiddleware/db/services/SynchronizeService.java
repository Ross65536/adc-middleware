package pt.inesctec.adcauthmiddleware.db.services;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.models.RepertoireModel;
import pt.inesctec.adcauthmiddleware.adc.resources.RearrangementSet;
import pt.inesctec.adcauthmiddleware.adc.resources.RepertoireSet;
import pt.inesctec.adcauthmiddleware.config.FieldConfig;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.models.StudyMappings;
import pt.inesctec.adcauthmiddleware.db.models.TemplateMappings;
import pt.inesctec.adcauthmiddleware.db.models.Templates;
import pt.inesctec.adcauthmiddleware.db.repository.RepertoireRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyMappingsRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;
import pt.inesctec.adcauthmiddleware.db.repository.TemplatesRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.models.UmaRegistrationResource;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResourceAttributes;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

/**
 * Service responsible for synchronizing dataset status of an ADC Service, the UMA/Authorization Service and this
 * Middleware's database.
 */
@Component
public class SynchronizeService {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(SynchronizeService.class);
    private static final Object SyncMonitor = new Object();

    @Autowired
    AdcClient adcClient;
    @Autowired
    DbService dbService;
    @Autowired
    UmaClient umaClient;
    @Autowired
    RepertoireRepository repertoireRepository;
    @Autowired
    StudyRepository studyRepository;
    @Autowired
    TemplatesRepository templateRepository;
    @Autowired
    StudyMappingsRepository studyMappingsRepository;

    @Autowired
    FieldConfig fieldConfig;

    /**
     * Synchronize endpoint entrypoint.
     * Clears out the middleware's Redis cache.
     *
     * @return true on 100% successful synchronization. On false synchronization may have failed for some resources.
     * @throws Exception on internal error.
     */
    @CacheEvict(cacheNames = {CacheConstants.STUDIES_CACHE_NAME, CacheConstants.REPERTOIRES_CACHE_NAME, CacheConstants.REARRANGEMENTS_CACHE_NAME}, allEntries = true)
    public boolean synchronize() throws Exception {
        synchronized (SynchronizeService.SyncMonitor) {
            return synchronizeGuts();
        }
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
    private boolean synchronizeGuts() throws Exception {
        Logger.info("Synchronizing DB and cache");

        // Start by querying the ADC service to determine available Repertoires
        var repertoireSearch = new AdcSearchRequest().addFields(
            RepertoireSet.ID_FIELD,
            RepertoireSet.UMA_ID_FIELD,
            RepertoireSet.STUDY_TITLE_FIELD
        );

        var repertoires = this.adcClient.searchRepertoires(repertoireSearch);

        CollectionsUtils.assertList(
            repertoires, e -> e.getRepertoireId() != null,
            "Repertoires response must have a " + RearrangementSet.REPERTOIRE_ID_FIELD
        );

        boolean syncSuccessful = true;

        // Sync studies
        // Build map of retrieved studies
        // Map [Study ID] -> Study Title
        var repositoryStudyMap = CollectionsUtils.toMapKeyByLatest(
            repertoires, RepertoireModel::getStudyId, RepertoireModel::getStudyTitle
        );

        if (!this.synchronizeStudies(repositoryStudyMap)) {
            syncSuccessful = false;
        }

        // sync cache
        dbService.deleteCache();
        if (!this.synchronizeRepertoires(repertoires)) {
            syncSuccessful = false;
        }

        Logger.info("Finished DB and cache synchronization");

        return syncSuccessful;
    }

    /**
     * Synchronize the studies state between keycloak, middleware and repository.
     * Will create, update, delete(*) UMA resources as needed.
     * Will also create the UMA ID to study ID DB associations as needed.
     * Some steps such as deletion or updating might fail, which will be skipped and false returned.
     *
     * <small>(*)UMA resources are not deleted, instead the resource type is set from "study" to "deleted".</small>
     *
     * @param repositoryStudyMap Map[Study ID] -> Study Title: map with the study IDs coming from the
     *                           ADC repository and their titles.
     * @return true on 100% successful synchronization.
     * @throws Exception on internal error.
     */
    private boolean synchronizeStudies(Map<String, String> repositoryStudyMap) throws Exception {
        boolean syncSuccessful = true;

        // Get UMA IDs present in the AuthZ server
        var listUmaIds = Set.of(this.umaClient.listUmaResources());

        // List of Study IDs in the ADC Repository
        var adcStudyIds = repositoryStudyMap.keySet();

        // List of Study IDs in the database
        var dbStudyIds = new HashSet<>(studyRepository.findAllMapByUmaId().values());

        // Delete dangling DB resources
        for (String danglingDbStudyId : Sets.difference(dbStudyIds, adcStudyIds)) {
            Logger.info("Deleting DB study with study ID: {}", danglingDbStudyId);
            try {
                this.studyRepository.deleteByStudyId(danglingDbStudyId);
            } catch (RuntimeException e) {
                syncSuccessful = false;
                Logger.error(
                    "Failed to delete DB study with study ID {}, because: {}",
                    danglingDbStudyId,
                    e.getMessage());
                Logger.debug("Stacktrace: ", e);
            }
        }
        this.studyRepository.flush();

        // Get UMA IDs in the database
        var dbUmaIds = studyRepository.findAllMapByUmaId().keySet();

        // Mark dangling UMA resources as "deleted" in the UMA Service
        for (String danglingUmaId : Sets.difference(listUmaIds, dbUmaIds)) {
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
                syncSuccessful = false;
                Logger.error(
                    "Failed to 'delete' UMA resource {}, because: {}", danglingUmaId, e.getMessage());
                Logger.debug("Stacktrace: ", e);
            }
        }

        // Delete dangling DB resources
        for (String danglingDbUmaId : Sets.difference(dbUmaIds, listUmaIds)) {
            Logger.info("Deleting DB study with uma ID: {}", danglingDbUmaId);
            try {
                this.studyRepository.deleteByUmaId(danglingDbUmaId);
            } catch (RuntimeException e) {
                syncSuccessful = false;
                Logger.error(
                    "Failed to delete DB study with UMA ID {}, because: {}",
                    danglingDbUmaId,
                    e.getMessage());
                Logger.debug("Stacktrace: ", e);
            }
        }

        // Add new resources
        var allUmaScopes = this.fieldConfig.getAllUmaScopes();

        dbStudyIds = new HashSet<>(studyRepository.findAllMapByUmaId().values());

        for (String newStudyId : Sets.difference(adcStudyIds, dbStudyIds)) {
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
                syncSuccessful = false;
                Logger.info("Resource {} not created", umaName);
                Logger.debug("Stacktrace: ", e);
                continue;
            }

            // Register Study in the Middleware's Database
            var study = new Study(newStudyId, createdUmaId);

            Logger.info("Creating DB study {}", study);
            try {
                this.studyRepository.saveAndFlush(study);
            } catch (RuntimeException e) {
                syncSuccessful = false;
                Logger.error("Failed to create DB study {} because: {}", study, e.getMessage());
                Logger.debug("Stacktrace: ", e);
            }

            // Register study with the default template mappings
            Templates defaultTemplate = templateRepository.findDefault();

            Logger.info("Registering newly found Study with default template: {}, id: {}",
                defaultTemplate.getName(), defaultTemplate.getId()
            );

            List<StudyMappings> studyMappings = new ArrayList<>();

            for (var mapping : defaultTemplate.getMappings()) {
                studyMappings.add(new StudyMappings(mapping, study));
            }

            studyMappingsRepository.saveAll(studyMappings);
        }

        // validate common resources
        for (String umaId : Sets.intersection(listUmaIds, dbUmaIds)) {
            try {
                var resource = this.umaClient.getResource(umaId);
                Set<String> actualResources = resource.getResourceScopes();

                if (actualResources.containsAll(allUmaScopes)) {
                    continue;
                }

                // update resource if scopes are not matching
                var updateResources = Sets.union(actualResources, allUmaScopes);
                var updateResource = new UmaRegistrationResource(
                    resource.getName(),
                    AdcConstants.UMA_STUDY_TYPE,
                    updateResources
                );

                Logger.info("Updating resource {}:{} with {}", umaId, resource, updateResource);

                this.umaClient.updateUmaResource(umaId, updateResource);
            } catch (Exception e) {
                syncSuccessful = false;
                Logger.error("Failed to check UMA resource {}, because: {}", umaId, e.getMessage());
                Logger.debug("Stacktrace: ", e);
            }
        }

        return syncSuccessful;
    }

    /**
     * Synchronize repertoire to study DB associations.
     * Some associations might fail, which will be skipped.
     *
     * @param backendRepertoires list of repertoire resources.
     * @return true on 100% successful synchronization.
     */
    private boolean synchronizeRepertoires(List<RepertoireModel> backendRepertoires) {
        boolean syncSuccessful = true;

        for (RepertoireModel repertoireIds : backendRepertoires) {
            var studyId = repertoireIds.getStudyId();
            var study = this.studyRepository.findByStudyId(studyId);
            if (study == null) {
                Logger.error("Invalid DB state, missing study {}, skipping", studyId);
                continue;
            }

            var repertoire = new Repertoire(repertoireIds.getRepertoireId(), study);
            if (!DbService.saveResource(this.repertoireRepository, repertoire)) {
                syncSuccessful = false;
            }
        }

        return syncSuccessful;
    }
}
