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
import pt.inesctec.adcauthmiddleware.db.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

public final class RepertoireSet extends AdcResourceSet {
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

    public RepertoireSet(AdcSearchRequest adcSearch, AdcClient adcClient, DbService dbService, CsvConfig csvConfig) {
        super(FieldClass.REPERTOIRE, adcSearch, adcClient, dbService, csvConfig);
    }

    /**
     * Obtain study UMA IDs that correspond to the requested resource in the ADC listing query.
     *
     * @return the UMA IDs
     * @throws Exception on error
     */
    @Override
    protected Set<String> getUmaIds() throws Exception  {
        return this.adcClient.searchRepertoireStudyIds(this.adcSearch).stream()
            .map(id -> this.dbService.getStudyUmaId(id))
            .collect(Collectors.toSet());
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response() throws Exception {
        if (this.adcSearch.isFacetsSearch()) {
            List<String> resourceIds = Collections.<String>emptyList();

            if (umaState.isEnabled()) {
                resourceIds = UmaUtils.filterFacets(
                    umaState.getResources(),
                    umaState.getScopes(),
                    (String umaId) -> CollectionsUtils.toSet(this.dbService.getUmaStudyId(umaId))
                );
            }

            return responseFilteredFacets(
                this.adcSearch,
                RepertoireSet.UMA_ID_FIELD,
                this.adcClient::searchRepertoiresAsStream,
                resourceIds,
                !umaState.getScopes().isEmpty());
        }

        Function<String, Set<String>> fieldMapper;

        if (umaState.isEnabled()) {
            fieldMapper = this.setupFieldMapper(fieldClass, RepertoireSet.UMA_ID_FIELD);
        } else {
            fieldMapper = this.setupPublicFieldMapper(fieldClass, RepertoireSet.UMA_ID_FIELD);
        }

        return responseFilteredJson(
            RepertoireSet.UMA_ID_FIELD,
            RepertoireSet.RESPONSE_FILTER_FIELD,
            fieldMapper.compose(this.dbService::getStudyUmaId),
            () -> this.adcClient.searchRepertoiresAsStream(adcSearch));
    }
}
