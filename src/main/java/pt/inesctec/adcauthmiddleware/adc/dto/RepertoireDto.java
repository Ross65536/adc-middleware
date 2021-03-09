package pt.inesctec.adcauthmiddleware.adc.dto;

import java.util.Set;
import java.util.function.Function;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.resources.AdcResource;
import pt.inesctec.adcauthmiddleware.adc.resources.RepertoireSet;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;

public class RepertoireDto extends AdcDto {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RepertoireDto.class);

    public RepertoireDto(String adcId, AdcClient adcClient, DbService dbService) {
        super(adcClient, dbService);
        this.adcId = adcId;
    }

    @Override
    public void processUma(String bearerToken, UmaFlow umaFlow) throws Exception {
        this.umaId = this.dbService.getRepertoireUmaId(this.adcId);

        if (this.umaId == null) {
            Logger.info("Non-existing repertoire with ID {}. Is database /synchronized?", this.adcId);
            throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
        }

        this.fieldMappings = this.dbService.studyMappingsRepository.findByUmaId(this.umaId);

        // Execute UMA Workflow only if it's enabled
        if (this.umaEnabled) {
            Set<String> scopes = UmaUtils.collectAccessScopes(this.fieldMappings);
            umaFlow.execute(bearerToken, this.umaId, scopes);
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response() throws Exception {
        Function<String, Set<String>> fieldMapper;

        if (this.umaEnabled) {
            fieldMapper = UmaUtils.buildFieldMapper(this.umaState.getResources(), this.fieldClass, csvConfig).compose(
                this.dbService::getStudyUmaId
            );
        }  else {
            fieldMapper = (s) -> { return csvConfig.getPublicFields(this.fieldClass); };
        }

        return AdcResource.responseFilteredJson(
            RepertoireSet.UMA_ID_FIELD,
            RepertoireSet.RESPONSE_FILTER_FIELD,
            fieldMapper,
            () -> this.adcClient.getRepertoireAsStream(this.repertoireId));
    }
}
