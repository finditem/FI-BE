CREATE TABLE `temperature_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `activity_type` varchar(30) NOT NULL,
  `source_id` bigint NOT NULL,
  `delta_tenths` int NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_temperature_activity_source` (`activity_type`, `source_id`),
  KEY `idx_temperature_activity_user_occurred` (`user_id`, `occurred_at`),
  CONSTRAINT `fk_temperature_activity_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_temperature` (
  `user_id` bigint NOT NULL,
  `temperature_tenths` int NOT NULL,
  `calculated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_temperature_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 배포 전 활동을 원장에 옮긴다. 게시글 삭제, 후기 숨김과 관계없이 활동 기록은 유지한다.
INSERT INTO `temperature_activity` (`user_id`, `activity_type`, `source_id`, `delta_tenths`, `occurred_at`)
SELECT `user_id`, 'FOUND_POST', `id`, 5, `created_at`
FROM `post`
WHERE `post_type` = 'FOUND' AND `user_id` IS NOT NULL;

INSERT INTO `temperature_activity` (`user_id`, `activity_type`, `source_id`, `delta_tenths`, `occurred_at`)
SELECT `helper_id`, 'RETURN_COMPLETED', `id`, 20, `created_at`
FROM `find_completion`
WHERE `helper_id` IS NOT NULL;

INSERT INTO `temperature_activity` (`user_id`, `activity_type`, `source_id`, `delta_tenths`, `occurred_at`)
SELECT `recipient_id`, 'REVIEW_RECEIVED', `id`, 10, `created_at`
FROM `review`
WHERE `recipient_id` IS NOT NULL;

-- 최초 온도 계산. 이후에는 주간 스케줄러가 같은 기준으로 다시 계산한다.
INSERT INTO `user_temperature` (`user_id`, `temperature_tenths`, `calculated_at`)
SELECT `user_id`,
       LEAST(990, GREATEST(0, 365 + COALESCE(SUM(CASE WHEN `occurred_at` >= NOW(6) - INTERVAL 1 YEAR THEN `delta_tenths` END), 0))),
       NOW(6)
FROM `temperature_activity`
GROUP BY `user_id`;
