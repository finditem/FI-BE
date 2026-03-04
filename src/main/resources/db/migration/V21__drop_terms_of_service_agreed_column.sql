SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'users'
                     AND COLUMN_NAME = 'terms_of_service_agreed');

SET @sql = IF(@col_exists > 0,
              'ALTER TABLE `users` DROP COLUMN `terms_of_service_agreed`',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
