CREATE TABLE access_scope (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL
);

CREATE TABLE adc_field_type (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL
);

INSERT INTO access_scope (name)
VALUES ('public'),
       ('raw_sequence'),
       ('statistics');

INSERT INTO adc_field_type (name)
VALUES ('repertoire'),
       ('rearrangement');