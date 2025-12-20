-- Rename user table to users to avoid confusion with SQL USER command
-- MySQL automatically updates foreign key constraints when renaming a table

-- Check if table exists before renaming
SET @table_exists := (SELECT COUNT(1) 
                     FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = DATABASE() 
                       AND TABLE_NAME = 'user');
SET @sql := IF(@table_exists > 0,
              'ALTER TABLE `user` RENAME TO `users`',
              'SELECT ''Table user does not exist, skipping RENAME'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Note: MySQL 5.7+ automatically updates foreign key constraints when renaming a table
-- If you encounter foreign key issues, you may need to manually update them

