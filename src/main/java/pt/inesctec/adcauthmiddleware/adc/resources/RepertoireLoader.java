package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

public class RepertoireLoader extends AdcResourceLoader {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RepertoireLoader.class);

    public RepertoireLoader(AdcClient adcClient, DbService dbService) {
        super(adcClient, dbService);
    }

    @Override
    public Set<String> loadUmaIds(String adcId) throws Exception {
        String umaId = this.dbService.getRepertoireUmaId(adcId);

        if (umaId == null) {
            Logger.info("Non-existing repertoire with ID {}. Is database /synchronized?", adcId);
            throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
        }

        return Set.of(umaId);
    }

    @Override
    public Set<String> loadUmaIds(AdcSearchRequest adcSearch) throws Exception {
        return this.adcClient.searchRepertoireStudyIds(adcSearch).stream()
            .map(id -> this.dbService.getStudyUmaId(id))
            .collect(Collectors.toSet());
    }

    @Override
    public void processUma(String bearerToken, UmaFlow umaFlow, ResourceState resourceState) throws Exception {
        resourceState.setEnabled(true);
        Set<String> scopes = this.dbService.getStudyMappingsRepository().findAccessScopeNamesByUmaIds(
            resourceState.getUmaIds()
        );
        resourceState.setFromUmaResources(umaFlow.execute(bearerToken, resourceState.getUmaIds(), scopes));
    }

    @Override
    public void loadFieldMappings(UmaConfig umaConfig, ResourceState resourceState) throws Exception {

    }
}
