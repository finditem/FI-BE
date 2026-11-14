-- 알림 유형별 수신 설정은 기본 켜짐이므로 *_enabled 기본값을 1로 둔다. Entity의 기본값과 같다.
-- 원래 V1의 정의였으나 dev와 prod에는 반영되지 않았고, V1 보정(#643)에서 실제 DB 기준으로 제거되었다.
-- 기존 행은 변경하지 않는다.

ALTER TABLE `notification_settings`
    ALTER COLUMN `browser_notification_enabled` SET DEFAULT b'1',
    ALTER COLUMN `category_enabled` SET DEFAULT b'1',
    ALTER COLUMN `chat_enabled` SET DEFAULT b'1',
    ALTER COLUMN `comment_enabled` SET DEFAULT b'1',
    ALTER COLUMN `favorite_enabled` SET DEFAULT b'1',
    ALTER COLUMN `inquiry_reply_enabled` SET DEFAULT b'1',
    ALTER COLUMN `notice_enabled` SET DEFAULT b'1',
    ALTER COLUMN `report_result_enabled` SET DEFAULT b'1';
