CREATE TABLE template_default (
    id SERIAL PRIMARY KEY,
    id_template BIGINT REFERENCES templates(id) NOT NULL
);

INSERT INTO template_default(id_template)
    SELECT id FROM templates
WHERE EXISTS (
    SELECT 1 FROM templates
) LIMIT 1;