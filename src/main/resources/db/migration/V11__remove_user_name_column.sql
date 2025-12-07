-- Remove `name` column from `user` table
-- This column was added in V3 but is no longer needed

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

