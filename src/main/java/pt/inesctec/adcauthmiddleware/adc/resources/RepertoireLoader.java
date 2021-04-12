package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.RepertoireConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

public class RepertoireLoader extends AdcResourceLoader {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RepertoireLoader.class);

    public RepertoireLoader(AdcClient adcClient, DbService dbService) {
        super("repertoire", adcClient, dbService);
    }

    @Override
    public void load(String adcId) throws Exception {
        super.load();

        String umaId = this.dbService.getRepertoireUmaId(adcId);

        if (umaId == null) {
            Logger.error("Non-existing repertoire with ID {}. Is database /synchronized?", adcId);
            throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
        }

        this.resourceState.setUmaIds(Set.of(umaId));
        this.resourceState.setScopes(this.loadScopes());
    }

    @Override
    public void load(AdcSearchRequest adcSearch) throws Exception {
        super.load();

        Set<String> umaIds = this.adcClient.searchRepertoireStudyIds(adcSearch).stream()
            .map(id -> this.dbService.getStudyUmaId(id))
            .collect(Collectors.toSet());

        this.resourceState.setUmaIds(umaIds);
        this.resourceState.setScopes(this.loadScopes(adcSearch));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response(String adcId) throws Exception {
        return AdcResourceLoader.responseFilteredJson(
            RepertoireConstants.UMA_ID_FIELD,
            RepertoireConstants.RESPONSE_FILTER_FIELD,
            this.resourceState.setupFieldMapper().compose(this.dbService::getStudyUmaId),
            () -> this.adcClient.getRepertoireAsStream(adcId));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response(AdcSearchRequest adcSearch) throws Exception {
        // TODO: Repertoire Facets
        if (adcSearch.isFacetsSearch()) {
            List<String> resourceIds = Collections.emptyList();

            if (resourceState.isUmaEnabled()) {
                resourceIds = UmaUtils.filterFacets(
                    resourceState.getResources().values(),
                    resourceState.getScopes(),
                    (String umaId) -> CollectionsUtils.toSet(this.dbService.getStudyIdByUmaId(umaId))
                );
            }

            return AdcResourceLoader.responseFilteredFacets(
                adcSearch,
                RepertoireConstants.UMA_ID_FIELD,
                this.adcClient::searchRepertoiresAsStream,
                resourceIds,
                resourceState.isUmaEnabled()
            );
        }

        return AdcResourceLoader.responseFilteredJson(
            RepertoireConstants.UMA_ID_FIELD,
            RepertoireConstants.RESPONSE_FILTER_FIELD,
            this.resourceState.setupFieldMapper().compose(this.dbService::getStudyUmaId),
            () -> this.adcClient.searchRepertoiresAsStream(adcSearch)
        );
    }
}
