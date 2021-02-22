CREATE TABLE template_mappings (
    id BIGSERIAL PRIMARY KEY,
    id_template BIGINT REFERENCES templates(id),
    id_adc_field INT REFERENCES adc_fields(id),
    id_access_scope INT REFERENCES access_scope(id),
    UNIQUE(id_template, id_adc_field, id_access_scope)
);