CREATE TABLE `find_completion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_post_id` bigint NOT NULL,
  `chat_room_id` bigint DEFAULT NULL,
  `requester_id` bigint DEFAULT NULL,
  `helper_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `entity_status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `deleted_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_find_completion_source_post` (`source_post_id`),
  KEY `idx_find_completion_chat_room` (`chat_room_id`),
  KEY `idx_find_completion_helper` (`helper_id`),
  KEY `idx_find_completion_requester` (`requester_id`),
  CONSTRAINT `fk_find_completion_chat_room` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_find_completion_requester` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_find_completion_helper` FOREIGN KEY (`helper_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `find_completion_id` bigint NOT NULL,
  `author_id` bigint DEFAULT NULL,
  `recipient_id` bigint DEFAULT NULL,
  `emotion` varchar(30) NOT NULL,
  `content` varchar(300) DEFAULT NULL,
  `hidden_by_recipient` bit(1) NOT NULL DEFAULT b'0',
  `revised_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `entity_status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `deleted_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_completion` (`find_completion_id`),
  KEY `idx_review_author_created` (`author_id`, `created_at` DESC, `id` DESC),
  KEY `idx_review_recipient_hidden_created` (`recipient_id`, `hidden_by_recipient`, `created_at` DESC, `id` DESC),
  CONSTRAINT `fk_review_completion` FOREIGN KEY (`find_completion_id`) REFERENCES `find_completion` (`id`),
  CONSTRAINT `fk_review_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_review_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review_help_type` (
  `review_id` bigint NOT NULL,
  `help_type` varchar(40) NOT NULL,
  PRIMARY KEY (`review_id`, `help_type`),
  CONSTRAINT `fk_review_help_type_review` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
