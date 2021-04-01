package pt.inesctec.adcauthmiddleware.adc.models.filters;

import java.util.List;
import java.util.Set;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.filters.content.FieldContent;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;

public class ContentFilterBase<T extends FieldContent> extends AdcFilter {
    private T content;

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public void validate(String field, List<AdcFields> validFields) throws AdcException {
        super.validate(field, validFields);
        FiltersUtils.assertNonNull(field + ".content", content);
        content.validate(field + ".content", validFields);
    }

    @Override
    public void loadFields(Set<String> fields) {
        content.loadFields(fields);
    }
}
