CREATE TABLE IF NOT EXISTS article_classification (
    article_classification_id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    taxonomy_dimension_id BIGINT NOT NULL,
    taxonomy_value_id BIGINT NOT NULL,
    confidence VARCHAR(255),
    proof TEXT,
    imported_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_article_classification_article
        FOREIGN KEY (article_id)
        REFERENCES article (article_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_article_classification_dimension
        FOREIGN KEY (taxonomy_dimension_id)
        REFERENCES taxonomy_dimension (taxonomy_dimension_id),
    CONSTRAINT fk_article_classification_value
        FOREIGN KEY (taxonomy_value_id)
        REFERENCES taxonomy_value (taxonomy_value_id),
    CONSTRAINT uk_article_classification_article_dimension_value
        UNIQUE (article_id, taxonomy_dimension_id, taxonomy_value_id)
);
