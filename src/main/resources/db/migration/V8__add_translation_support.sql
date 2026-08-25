ALTER TABLE users
    ADD COLUMN preferred_language VARCHAR(10) DEFAULT 'KO';

UPDATE users
SET preferred_language = 'KO'
WHERE preferred_language IS NULL;

ALTER TABLE users
    MODIFY COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'KO';

CREATE TABLE IF NOT EXISTS `post_translation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `language_code` varchar(10) NOT NULL,
  `translated_title` varchar(100) NOT NULL,
  `translated_content` varchar(1000) NOT NULL,
  `source_content_hash` varchar(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_translation` (`post_id`, `language_code`),
  CONSTRAINT `fk_post_translation_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
