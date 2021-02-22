package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;

public final class RearrangementSet extends AdcResourceSet {
    /**
     * The rearrangement's ID field name.
     */
    public static final String ID_FIELD = "sequence_id";

    /**
     * The rearrangement's repertoire ID field name (the parent resource's ID).
     */
    public static final String REPERTOIRE_ID_FIELD = "repertoire_id";

    /**
     * The ADC document (JSON object) response's field name for the rearrangement list.
     */
    public static final String RESPONSE_FILTER_FIELD = "Rearrangement";

    public RearrangementSet(
        AdcSearchRequest adcSearch,
        AdcClient adcClient,
        DbRepository dbRepository,
        CsvConfig csvConfig
    ) {
        super(FieldClass.REARRANGEMENT, adcSearch, adcClient, dbRepository, csvConfig);
    }

    /**
     * Function to obtain the unique study UMA IDs that correspond to the user's repertoire ADC query search.
     *
     * @return the UMA IDs
     * @throws Exception on error
     */
    @Override
    public Set<String> getUmaIds() throws Exception  {
        return this.adcClient.getRearrangementRepertoireModel(this.adcSearch).stream()
            .map(id -> this.dbRepository.getRepertoireUmaId(id))
            .collect(Collectors.toSet());
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response() throws Exception {
        if (adcSearch.isFacetsSearch()) {
            List<String> resourceIds = Collections.<String>emptyList();

            if (umaState.isEnabled()) {
                resourceIds = UmaUtils.filterFacets(
                    umaState.getResources(), umaState.getScopes(), this.dbRepository::getUmaRepertoireModel
                );
            }

            return responseFilteredFacets(
                adcSearch,
                RearrangementSet.REPERTOIRE_ID_FIELD,
                this.adcClient::searchRearrangementsAsStream,
                resourceIds,
                !umaState.getScopes().isEmpty());
        }

        // TODO: Could this be a class parameter? Maybe move it to enableUma? Too specific?
        Function<String, Set<String>> fieldMapper;

        if (umaState.isEnabled()) {
            fieldMapper = setupFieldMapper(fieldClass, RearrangementSet.REPERTOIRE_ID_FIELD);
        } else {
            fieldMapper = setupPublicFieldMapper(fieldClass, RearrangementSet.REPERTOIRE_ID_FIELD);
        }

        if (adcSearch.isJsonFormat()) {
            return responseFilteredJson(
                RearrangementSet.REPERTOIRE_ID_FIELD,
                RearrangementSet.RESPONSE_FILTER_FIELD,
                fieldMapper.compose(this.dbRepository::getRepertoireUmaId),
                () -> this.adcClient.searchRearrangementsAsStream(adcSearch));
        }

        var requestedFieldTypes = RearrangementSet.getRegularSearchRequestedFieldsAndTypes(
            adcSearch, FieldClass.REARRANGEMENT, this.csvConfig
        );

        return responseFilteredTsv(
            RearrangementSet.REPERTOIRE_ID_FIELD,
            RearrangementSet.RESPONSE_FILTER_FIELD,
            fieldMapper.compose(this.dbRepository::getRepertoireUmaId),
            () -> this.adcClient.searchRearrangementsAsStream(adcSearch),
            requestedFieldTypes);
    }

    /**
     * Obtain the fields and their types from the user's regular ADC request. Should check previously that the request is not facets.
     *
     * @param request    The user's ADC query
     * @param fieldClass the resource type
     * @return the fields and types
     */
    // TODO: Change this to non-static, remove AdcSearchRequest as a parameter.
    // TODO: Horrible function name. Review.
    public static Map<String, FieldType> getRegularSearchRequestedFieldsAndTypes(
        AdcSearchRequest request, FieldClass fieldClass, CsvConfig csvConfig
    ) {
        var requestedFields = request.getRequestedFields(fieldClass, csvConfig);
        Map<String, FieldType> allFields = csvConfig.getFieldsTypes(fieldClass);
        return CollectionsUtils.intersectMapWithSet(allFields, requestedFields);
    }
}
