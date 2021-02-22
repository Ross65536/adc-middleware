package pt.inesctec.adcauthmiddleware.controllers;

import java.util.Set;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import pt.inesctec.adcauthmiddleware.HttpException;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Delayer;

@RequestMapping("${app.airrBasepath}")
public abstract class AdcController {
    @Autowired
    protected AppConfig appConfig;
    @Autowired
    protected CsvConfig csvConfig;
    @Autowired
    protected AdcClient adcClient;

    protected static org.slf4j.Logger Logger;
    protected Delayer repertoiresDelayer;
    protected Delayer rearrangementsDelayer;

    /**
     * Validate that the user's ADC query is semantically correct. Also enforces disabled features as set in the configuration.
     *
     * @param adcSearch  the user's ADC query
     * @param fieldClass the resource type
     * @param tsvEnabled whether TSV is enabled for the considered endpoint.
     * @throws HttpException on validation error
     */
    protected void validateAdcSearch(
        AdcSearchRequest adcSearch, FieldClass fieldClass, boolean tsvEnabled) throws HttpException {

        if (adcSearch.isFacetsSearch() && !this.appConfig.isFacetsEnabled()) {
            throw SpringUtils.buildHttpException(
                HttpStatus.NOT_IMPLEMENTED,
                "Invalid input JSON: 'facets' support for current repository not enabled");
        }

        if (adcSearch.getFilters() != null && !this.appConfig.isAdcFiltersEnabled()) {
            throw SpringUtils.buildHttpException(
                HttpStatus.NOT_IMPLEMENTED,
                "Invalid input JSON: 'filters' support for current repository not enabled");
        }

        var filtersBlacklist = this.appConfig.getFiltersOperatorsBlacklist();
        Set<String> actualFiltersOperators = adcSearch.getFiltersOperators();
        Sets.SetView<String> operatorDiff = Sets.intersection(filtersBlacklist, actualFiltersOperators);
        if (!operatorDiff.isEmpty()) {
            throw SpringUtils.buildHttpException(
                HttpStatus.NOT_IMPLEMENTED,
                "Invalid input JSON: 'filters' operators: "
                    + CollectionsUtils.toString(operatorDiff)
                    + " are blacklisted");
        }

        final boolean isTsv = !adcSearch.isJsonFormat();
        if (isTsv && !tsvEnabled) {
            throw SpringUtils.buildHttpException(
                HttpStatus.UNPROCESSABLE_ENTITY, "TSV format not enabled for this endpoint");
        }

        var fieldTypes = this.csvConfig.getFieldsTypes(fieldClass);
        var requestedFields = adcSearch.getRequestedFields(FieldClass.REARRANGEMENT, this.csvConfig);
        try {
            AdcSearchRequest.validate(adcSearch, fieldTypes, requestedFields);
        } catch (AdcException e) {
            throw SpringUtils.buildHttpException(
                HttpStatus.UNPROCESSABLE_ENTITY, "Invalid input JSON: " + e.getMessage());
        }
    }
}
