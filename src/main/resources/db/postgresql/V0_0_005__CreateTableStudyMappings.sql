CREATE TABLE study_mappings (
    id BIGSERIAL PRIMARY KEY,
    id_study BIGINT REFERENCES study(id) ON DELETE CASCADE,
    id_access_scope INT REFERENCES access_scope(id),
    id_adc_field INT REFERENCES adc_fields(id),
    UNIQUE(id_study, id_adc_field)
);