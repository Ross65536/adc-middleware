package pt.inesctec.adcauthmiddleware.adc.old;

import java.util.Set;
import java.util.function.Function;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;

public final class RepertoireResourceOld extends AdcResourceOld {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RepertoireResourceOld.class);
    private final String repertoireId;

    public RepertoireResourceOld(String repertoireId, AdcClient adcClient, DbService dbService, CsvConfig csvConfig) {
        super(FieldClass.REPERTOIRE, adcClient, dbService, csvConfig);
        this.repertoireId = repertoireId;
    }

    @Override
    protected Set<String> getUmaIds() throws Exception {
        var umaId = this.dbService.getRepertoireUmaId(repertoireId);

        if (umaId == null) {
            Logger.info("Non-existing repertoire with ID {}. Is database /synchronized?", repertoireId);
            throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
        }

        return Set.of(umaId);
    }

    @Override
    protected Set<String> getUmaScopes() {
        return this.csvConfig.getUmaScopes(FieldClass.REPERTOIRE);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> response() throws Exception {
        Function<String, Set<String>> fieldMapper;

        if (this.umaStateOld.isEnabled()) {
            fieldMapper = UmaUtils.buildFieldMapper(
                this.umaStateOld.getResources(), this.fieldClass, csvConfig
            ).compose(this.dbService::getStudyUmaId);
        }
        else {
            fieldMapper = (s) -> {
                return csvConfig.getPublicFields(this.fieldClass);
            };
        }

        return AdcResourceOld.responseFilteredJson(
            RepertoireSet.UMA_ID_FIELD,
            RepertoireSet.RESPONSE_FILTER_FIELD,
            fieldMapper,
            () -> this.adcClient.getRepertoireAsStream(this.repertoireId));
    }
}
