ALTER TABLE `notification_settings`
    ALTER COLUMN `browser_notification_enabled` SET DEFAULT b'1',
    ALTER COLUMN `category_enabled` SET DEFAULT b'1',
    ALTER COLUMN `chat_enabled` SET DEFAULT b'1',
    ALTER COLUMN `comment_enabled` SET DEFAULT b'1',
    ALTER COLUMN `favorite_enabled` SET DEFAULT b'1',
    ALTER COLUMN `inquiry_reply_enabled` SET DEFAULT b'1',
    ALTER COLUMN `notice_enabled` SET DEFAULT b'1',
    ALTER COLUMN `report_result_enabled` SET DEFAULT b'1';
