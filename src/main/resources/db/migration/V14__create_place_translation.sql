CREATE TABLE place_translation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    place_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    translated_name VARCHAR(100) NULL,
    translated_address VARCHAR(510) NULL,
    translated_station VARCHAR(200) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_place_translation (place_id, language_code),
    CONSTRAINT fk_place_translation_place FOREIGN KEY (place_id) REFERENCES place (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
