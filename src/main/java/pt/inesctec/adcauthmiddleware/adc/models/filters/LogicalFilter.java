package pt.inesctec.adcauthmiddleware.adc.models.filters;

import java.util.List;
import java.util.Set;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

/**
 * An adc logical filter, like 'and', 'or'.
 */
public class LogicalFilter extends AdcFilter {
    private List<AdcFilter> content;

    public List<AdcFilter> getContent() {
        return content;
    }

    public void setContent(List<AdcFilter> content) {
        this.content = content;
    }

    @Override
    public void validate(String field, List<AdcFields> validFields) throws AdcException {
        super.validate(field, validFields);
        FiltersUtils.assertNonNull(field + ".content", content);
        for (int i = 0; i < content.size(); i++) {
            content.get(i).validate(field + ".content." + i, validFields);
        }
    }

    @Override
    public void loadFields(Set<String> fields) {
        for (AdcFilter adcFilter : content) {
            adcFilter.loadFields(fields);
        }
    }

    @Override
    public void loadOperators(Set<String> operators) {
        super.loadOperators(operators);
        for (AdcFilter adcFilter : content) {
            adcFilter.loadOperators(operators);
        }
    }
}
