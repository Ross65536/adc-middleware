package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

public final class RepertoireResource extends AdcResource {
    /**
     * The repertoire's ID field name.
     */
    public static final String ID_FIELD = "repertoire_id";

    /**
     * The repertoire's study ID field name (the parent resource's ID). The comma separator is hardcoded in other parts of code.
     */
    public static final String UMA_ID_FIELD = "study.study_id";

    /**
     * The repertoire's study title field name. The comma separator is hardcoded in other parts of code.
     */
    public static final String STUDY_TITLE_FIELD = "study.study_title";

    /**
     * The ADC document (JSON object) response's field name for the repertoire list.
     */
    public static final String RESPONSE_FILTER_FIELD = "Repertoire";

    /**
     * The repertoire's study first fragment of the field. Used by JSON parser. In the code 2 levels are specified for the study ID field.
     */
    public static final String STUDY_BASE = "study";

    /**
     * The repertoire's study's study ID second fragment name.
     */
    public static final String STUDY_ID_BASE = "study_id";

    /**
     * The repertoire's study's study title second fragment name.
     */
    public static final String STUDY_TITLE_BASE = "study_title";

    public RepertoireResource(AdcSearchRequest adcSearch, AdcClient adcClient, DbRepository dbRepository, CsvConfig csvConfig) {
        super(FieldClass.REPERTOIRE, adcSearch, adcClient, dbRepository, csvConfig);
    }

    /**
     * Obtain study UMA IDs that correspond to the requested resource in the ADC listing query.
     *
     * @return the UMA IDs
     * @throws Exception on error
     */
    @Override
    public Set<String> getUmaIds() throws Exception  {
        return this.adcClient.getRepertoireStudyIds(this.adcSearch).stream()
            .map(id -> this.dbRepository.getStudyUmaId(id))
            .collect(Collectors.toSet());
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response() throws Exception {
        if (this.adcSearch.isFacetsSearch()) {
            List<String> resourceIds = Collections.<String>emptyList();

            if (umaEnabled) {
                resourceIds = UmaUtils.filterFacets(
                    umaResources,
                    umaScopes,
                    (String umaId) -> CollectionsUtils.toSet(this.dbRepository.getUmaStudyId(umaId))
                );
            }

            return responseFilteredFacets(
                this.adcSearch,
                RepertoireResource.UMA_ID_FIELD,
                this.adcClient::searchRepertoiresAsStream,
                resourceIds,
                !umaScopes.isEmpty());
        }

        Function<String, Set<String>> fieldMapper;

        if (umaEnabled) {
            fieldMapper = this.setupFieldMapper(fieldClass, RepertoireResource.UMA_ID_FIELD);
        } else
        {
            fieldMapper = this.setupPublicFieldMapper(fieldClass, RepertoireResource.UMA_ID_FIELD);
        }

        return responseFilteredJson(
            RepertoireResource.UMA_ID_FIELD,
            RepertoireResource.RESPONSE_FILTER_FIELD,
            fieldMapper.compose(this.dbRepository::getStudyUmaId),
            () -> this.adcClient.searchRepertoiresAsStream(adcSearch));
    }
}
