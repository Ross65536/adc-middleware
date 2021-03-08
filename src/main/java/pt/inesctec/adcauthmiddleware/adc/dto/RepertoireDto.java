package pt.inesctec.adcauthmiddleware.adc.dto;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.controllers.SpringUtils;
import pt.inesctec.adcauthmiddleware.db.repository.StudyMappingsRepository;
import pt.inesctec.adcauthmiddleware.db.services.DbService;

public class RepertoireDto extends AdcDto {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RepertoireDto.class);

    public RepertoireDto(String adcId, AdcClient adcClient, DbService dbService) {
        super(adcClient, dbService);
        this.adcId = adcId;
    }

    protected void setupUma() throws Exception {
        this.umaId = this.dbService.getRepertoireUmaId(this.adcId);

        if (this.umaId == null) {
            Logger.info("Non-existing repertoire with ID {}. Is database /synchronized?", this.adcId);
            throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
        }

        this.scopes = this.dbService.studyMappingsRepository.findAccessScopesByUmaId(this.umaId);
    }
}
