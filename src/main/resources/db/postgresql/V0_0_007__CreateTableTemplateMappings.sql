CREATE TABLE template_mappings (
    id SERIAL PRIMARY KEY,
    id_template BIGINT REFERENCES templates(id),
    id_access_scope INT REFERENCES access_scope(id),
    id_adc_field INT REFERENCES adc_fields(id),
    UNIQUE(id_template, id_adc_field)
);