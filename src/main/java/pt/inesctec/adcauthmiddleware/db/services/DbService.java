package pt.inesctec.adcauthmiddleware.db.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.RearrangementModel;
import pt.inesctec.adcauthmiddleware.adc.resources.RearrangementSet;
import pt.inesctec.adcauthmiddleware.db.models.Repertoire;
import pt.inesctec.adcauthmiddleware.db.repository.RepertoireRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;

/**
 * Responsible for managing, synchronizing the middleware's DB, cache and the Authorization Service.
 */
@Component
public class DbService {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(DbService.class);

    @Autowired
    AdcClient adcClient;
    @Autowired
    StudyRepository studyRepository;
    @Autowired
    RepertoireRepository repertoireRepository;

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
     * Get UMA ID given an ADC Study ID. Cached.
     *
     * @param studyId the study ID
     * @return UMA ID. Null if no mapping exists.
     */
    @Cacheable(value = CacheConstants.STUDIES_CACHE_NAME, unless = "#result==null")
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
    @Cacheable(value = CacheConstants.REPERTOIRES_CACHE_NAME, unless = "#result==null")
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
    @Cacheable(value = CacheConstants.REARRANGEMENTS_CACHE_NAME, unless = "#result==null")
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
            Logger.error("Response's rearrangement can't have a null " + RearrangementSet.ID_FIELD);
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
     * Delete DB associations between repertoire IDs and study IDs.
     */
    protected void deleteCache() {
        this.repertoireRepository.deleteAll();
    }
}
