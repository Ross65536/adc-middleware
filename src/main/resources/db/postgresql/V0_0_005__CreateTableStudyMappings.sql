CREATE TABLE study_mappings (
    id SERIAL PRIMARY KEY,
    id_study BIGINT REFERENCES study(id),
    id_access_scope INT REFERENCES access_scope(id),
    id_adc_field INT REFERENCES adc_fields(id),
    UNIQUE(id_study, id_adc_field)
);