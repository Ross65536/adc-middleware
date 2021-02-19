package pt.inesctec.adcauthmiddleware.adc.resources;

import java.util.Set;
import java.util.function.Function;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaUtils;

public final class RepertoireResource extends AdcResource {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RepertoireResource.class);
    private String repertoireId;

    public RepertoireResource(String repertoireId, AdcClient adcClient, DbRepository dbRepository, CsvConfig csvConfig) {
        super(FieldClass.REPERTOIRE, adcClient, dbRepository, csvConfig);
        this.repertoireId = repertoireId;
    }

    @Override
    protected Set<String> getUmaIds() throws Exception {
        var umaId = this.dbRepository.getRepertoireUmaId(repertoireId);

        if (umaId == null) {
            Logger.info("Non-existing repertoire with ID {}. Is database /synchronized?", repertoireId);
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

        if (this.umaState.isEnabled()) {
            fieldMapper = UmaUtils.buildFieldMapper(
                this.umaState.getResources(), this.fieldClass, csvConfig
            ).compose(this.dbRepository::getStudyUmaId);
        }  else {
            fieldMapper = (s) -> {
                return csvConfig.getPublicFields(this.fieldClass);
            };
        }

        return AdcResource.responseFilteredJson(
            RepertoireSet.UMA_ID_FIELD,
            RepertoireSet.RESPONSE_FILTER_FIELD,
            fieldMapper,
            () -> this.adcClient.getRepertoireAsStream(this.repertoireId));
    }
}
