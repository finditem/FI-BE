-- DB 시간 저장 기준을 UTC에서 Asia/Seoul로 전환합니다.
-- hibernate.jdbc.time_zone 변경과 함께 배포해야 하며, 이 파일을 DB에서 직접 실행하지 않습니다.
-- 제외: post_statistics.started_at, user_statistics.started_at (초기화 스크립트가 이미 +09:00으로 저장)
-- NULL 값은 그대로 NULL로 남습니다.

UPDATE `blocked_user`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `chat_message`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `chat_room`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `chat_room_participant`
SET `first_unread_at` = `first_unread_at` + INTERVAL 9 HOUR,
    `last_message_sent_at` = `last_message_sent_at` + INTERVAL 9 HOUR,
    `last_reminded_at` = `last_reminded_at` + INTERVAL 9 HOUR;

UPDATE `comment`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `customer_inquiry`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `find_completion`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `inquiry_comment`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `ip_blacklist`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `notice`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `notice_comment`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `notice_comment_like`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `notice_like`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `notification`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `notification_settings`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `place`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `place_business_hour`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `place_favorite`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `place_operation_period`
SET `closing_at` = `closing_at` + INTERVAL 9 HOUR,
    `created_at` = `created_at` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `place_translation`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `post`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `date` = `date` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `post_favorite`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `post_translation`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `push_subscriptions`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `report`
SET `answered_at` = `answered_at` + INTERVAL 9 HOUR,
    `created_at` = `created_at` + INTERVAL 9 HOUR,
    `resolved_at` = `resolved_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `report_answer_image`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `review`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `revised_at` = `revised_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

UPDATE `social_accounts`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `temperature_activity`
SET `occurred_at` = `occurred_at` + INTERVAL 9 HOUR;

UPDATE `user_category`
SET `created_at` = `created_at` + INTERVAL 9 HOUR;

UPDATE `user_temperature`
SET `calculated_at` = `calculated_at` + INTERVAL 9 HOUR;

UPDATE `users`
SET `created_at` = `created_at` + INTERVAL 9 HOUR,
    `deleted_at` = `deleted_at` + INTERVAL 9 HOUR,
    `temporary_password_expires_at` = `temporary_password_expires_at` + INTERVAL 9 HOUR,
    `updated_at` = `updated_at` + INTERVAL 9 HOUR;

-- 영업시간(TIME)은 24시를 넘으면 0시부터 다시 시작합니다.
UPDATE `place_business_hour`
SET `start_time` = SEC_TO_TIME(MOD(TIME_TO_SEC(`start_time`) + 9 * 3600, 86400)),
    `end_time` = SEC_TO_TIME(MOD(TIME_TO_SEC(`end_time`) + 9 * 3600, 86400));
