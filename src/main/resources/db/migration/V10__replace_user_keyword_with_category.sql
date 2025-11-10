-- Replace legacy user_keyword table with user_category subscriptions

-- Create user_category table if it does not exist
CREATE TABLE IF NOT EXISTS user_category (
    user_category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_user_category_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_category UNIQUE (user_id, category),
    INDEX idx_user_category_user (user_id),
    INDEX idx_user_category_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate distinct user/category pairs from user_keyword if the table exists
SET @table_exists := (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_keyword'
);

SET @sql := IF(@table_exists = 1,
    'INSERT IGNORE INTO user_category (user_id, category, created_at)
     SELECT uk.user_id,
            uk.category,
            COALESCE(MIN(uk.created_at), NOW())
     FROM user_keyword uk
     GROUP BY uk.user_id, uk.category',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop legacy user_keyword table when present
SET @sql := IF(@table_exists = 1,
    'DROP TABLE user_keyword',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure notification settings table has category toggle and remove keyword toggle
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_settings'
      AND COLUMN_NAME = 'category_enabled'
);

SET @sql := IF(@col_exists = 0,
    'ALTER TABLE notification_settings
        ADD COLUMN category_enabled BOOLEAN DEFAULT TRUE COMMENT ''카테고리 알림'' AFTER notice_enabled',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_settings'
      AND COLUMN_NAME = 'keyword_enabled'
);

SET @sql := IF(@col_exists = 1,
    'ALTER TABLE notification_settings DROP COLUMN keyword_enabled',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

