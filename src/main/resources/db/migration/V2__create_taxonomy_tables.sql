CREATE TABLE IF NOT EXISTS taxonomy_dimension (
    taxonomy_dimension_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    description TEXT,
    taxonomy_description TEXT,
    generation_date VARCHAR(255),
    source_papers_count INTEGER,
    source_papers_json TEXT,
    CONSTRAINT uk_taxonomy_dimension_normalized_name UNIQUE (normalized_name)
);

CREATE TABLE IF NOT EXISTS taxonomy_value (
    taxonomy_value_id BIGSERIAL PRIMARY KEY,
    label TEXT NOT NULL,
    normalized_value TEXT NOT NULL,
    defined_by_json TEXT,
    dimension_id BIGINT NOT NULL,
    CONSTRAINT fk_taxonomy_value_dimension
        FOREIGN KEY (dimension_id)
        REFERENCES taxonomy_dimension (taxonomy_dimension_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_taxonomy_value_dimension_normalized_value
        UNIQUE (dimension_id, normalized_value)
);
