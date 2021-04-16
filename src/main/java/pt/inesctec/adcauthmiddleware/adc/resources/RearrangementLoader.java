package pt.inesctec.adcauthmiddleware.adc.resources;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.RearrangementConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.utils.ThrowingSupplier;

public class RearrangementLoader extends AdcResourceLoader {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RearrangementLoader.class);

    public RearrangementLoader(AdcClient adcClient, DbService dbService) {
        super("rearrangement", adcClient, dbService);
    }

    @Override
    public void load(String adcId) throws Exception {
        super.load();

        String umaId = this.dbService.getRearrangementUmaId(adcId);

        if (umaId == null) {
            Logger.error("Non-existing rearrangement with ID {}. Is database /synchronized?", adcId);
            throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
        }

        this.resourceState.setUmaIds(Set.of(umaId));
        this.resourceState.setScopes(this.loadScopes());
    }

    @Override
    public void load(AdcSearchRequest adcSearch) throws Exception {
        super.load();

        Set<String> umaIds = this.adcClient.searchRearrangementRepertoireIds(adcSearch).stream()
            .map(id -> this.dbService.getRepertoireUmaId(id))
            .collect(Collectors.toSet());

        this.resourceState.setUmaIds(umaIds);
        this.resourceState.setScopes(this.loadScopes(adcSearch));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response(String adcId) throws Exception {
        return AdcResourceLoader.responseFilteredJson(
            RearrangementConstants.REPERTOIRE_ID_FIELD,
            RearrangementConstants.RESPONSE_FILTER_FIELD,
            this.resourceState.setupFieldMapper().compose(this.dbService::getRepertoireUmaId),
            () -> this.adcClient.getRearrangementAsStream(adcId));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response(AdcSearchRequest adcSearch) throws Exception {
        if (adcSearch.isFacetsSearch()) {
            List<String> resourceIds = loadFacetIds(
                adcSearch,
                resourceState.getResources(),
                (String umaId) -> this.dbService.getRepertoireIdsByUmaId(umaId)
            );

            return AdcResourceLoader.responseFilteredFacets(
                adcSearch,
                RearrangementConstants.REPERTOIRE_ID_FIELD,
                this.adcClient::searchRearrangementsAsStream,
                resourceIds
            );
        }

        if (adcSearch.isTsvFormat()) {
            // This may seem counter-intuitive, but if we're dealing with a TSV format search,
            // we're actually going to request data in JSON format, and then convert it to TSV
            // This allows us to reuse the filtering code also used for filtering standard JSONs
            var adcSearchClone = adcSearch.queryClone();
            adcSearchClone.setFormat("json");

            var headers = new ArrayList<>(adcSearch.getRequestedFields());

            if (headers.isEmpty()) {
                var headerFields = this.dbService.getAdcFieldsRepository().findByType(this.adcFieldType);
                headers = headerFields.stream().map(AdcFields::getName).collect(Collectors.toCollection(ArrayList::new));
            }

            return responseFilteredTsv(
                RearrangementConstants.REPERTOIRE_ID_FIELD,
                RearrangementConstants.RESPONSE_FILTER_FIELD,
                headers,
                this.resourceState.setupFieldMapper().compose(this.dbService::getRepertoireUmaId),
                () -> this.adcClient.searchRearrangementsAsStream(adcSearchClone)
            );
        }

        return AdcResourceLoader.responseFilteredJson(
            RearrangementConstants.REPERTOIRE_ID_FIELD,
            RearrangementConstants.RESPONSE_FILTER_FIELD,
            this.resourceState.setupFieldMapper().compose(this.dbService::getRepertoireUmaId),
            () -> this.adcClient.searchRearrangementsAsStream(adcSearch)
        );
    }

    /**
     * Build TSV streaming, filtered, response.
     *
     * @param resourceId          the resource's ID fields
     * @param responseFilterField the response's field where the resources are set
     * @param fieldMapper         the ID to granted fields mapper
     * @param adcRequest          the ADC request producer.
     * @return streaming response
     * @throws Exception on error
     */
    public static ResponseEntity<StreamingResponseBody> responseFilteredTsv(
        String resourceId,
        String responseFilterField,
        List<String> headers,
        Function<String, Set<String>> fieldMapper,
        ThrowingSupplier<InputStream, Exception> adcRequest
    ) throws Exception {
        var response = SpringUtils.catchForwardingError(adcRequest);
        var filter = new FieldsFilter(fieldMapper, resourceId);
        var mapper = AdcJsonDocumentParser.buildTsvMapper(
            response, responseFilterField, headers, filter
        );
        return SpringUtils.buildTsvStream(mapper);
    }
}
