CREATE TABLE repertoire (
    id BIGSERIAL PRIMARY KEY,
    repertoire_id VARCHAR NOT NULL UNIQUE,
    study_id BIGINT NOT NULL REFERENCES study(id) ON DELETE CASCADE
);

COMMENT ON TABLE study IS 'Maps Repertoire ID (from the resource server) with a Study ID (study table on this database)';