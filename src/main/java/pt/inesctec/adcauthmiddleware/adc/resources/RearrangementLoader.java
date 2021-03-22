package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.old.RepertoireSetOld;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

public class RearrangementLoader extends AdcResourceLoader {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RearrangementLoader.class);

    public RearrangementLoader(AdcClient adcClient, DbService dbService) {
        super("rearrangement", adcClient, dbService);
    }

    @Override
    public void load(String adcId) throws Exception {
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
        Set<String> umaIds = this.adcClient.searchRearrangementRepertoireIds(adcSearch).stream()
            .map(id -> this.dbService.getRepertoireUmaId(id))
            .collect(Collectors.toSet());

        this.resourceState.setUmaIds(umaIds);
        this.resourceState.setScopes(this.loadScopes(adcSearch));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response(String adcId) throws Exception {
        return AdcResourceLoader.responseFilteredJson(
            RepertoireSetOld.UMA_ID_FIELD,
            RepertoireSetOld.RESPONSE_FILTER_FIELD,
            this.resourceState.setupFieldMapper().compose(this.dbService::getStudyUmaId),
            () -> this.adcClient.getRepertoireAsStream(adcId));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response(AdcSearchRequest adcSearch) throws Exception {
        return responseFilteredJson(
            RepertoireSetOld.UMA_ID_FIELD,
            RepertoireSetOld.RESPONSE_FILTER_FIELD,
            this.resourceState.setupFieldMapper().compose(this.dbService::getStudyUmaId),
            () -> this.adcClient.searchRepertoiresAsStream(adcSearch));
    }
}
