CREATE TABLE public.study (
	id SERIAL PRIMARY KEY,
	is_public bool NOT NULL DEFAULT false,
	study_id VARCHAR NOT NULL UNIQUE,
	uma_id VARCHAR NOT NULL UNIQUE
);