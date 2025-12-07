-- Remove unused columns from `user` table
-- These columns were added in V2/V3 but are no longer needed

-- Remove name column
SET @col_exists := (SELECT COUNT(1) 
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'user' 
                      AND COLUMN_NAME = 'name');
SET @sql := IF(@col_exists > 0,
               'ALTER TABLE `user` DROP COLUMN `name`',
               'SELECT ''Column name does not exist, skipping DROP'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove phone_number index first (before dropping column)
SET @idx_exists := (SELECT COUNT(1)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'user'
                      AND index_name = 'ux_user_phone_number');
SET @sql := IF(@idx_exists > 0,
               'ALTER TABLE `user` DROP INDEX ux_user_phone_number',
               'SELECT ''Index ux_user_phone_number does not exist, skipping DROP'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove phone_number column
SET @col_exists := (SELECT COUNT(1) 
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'user' 
                      AND COLUMN_NAME = 'phone_number');
SET @sql := IF(@col_exists > 0,
               'ALTER TABLE `user` DROP COLUMN `phone_number`',
               'SELECT ''Column phone_number does not exist, skipping DROP'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove phone_verified column
SET @col_exists := (SELECT COUNT(1) 
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'user' 
                      AND COLUMN_NAME = 'phone_verified');
SET @sql := IF(@col_exists > 0,
               'ALTER TABLE `user` DROP COLUMN `phone_verified`',
               'SELECT ''Column phone_verified does not exist, skipping DROP'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove trust_score column
SET @col_exists := (SELECT COUNT(1) 
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'user' 
                      AND COLUMN_NAME = 'trust_score');
SET @sql := IF(@col_exists > 0,
               'ALTER TABLE `user` DROP COLUMN `trust_score`',
               'SELECT ''Column trust_score does not exist, skipping DROP'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

