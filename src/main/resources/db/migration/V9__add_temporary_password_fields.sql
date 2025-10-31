-- User 테이블에 임시 비밀번호 관련 필드 추가

-- original_password: 임시 비밀번호 발급 시 원래 비밀번호 보관
SET @col_name := 'original_password';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `original_password` VARCHAR(255) NULL';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- temporary_password: 임시 비밀번호 (해시값)
SET @col_name := 'temporary_password';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `temporary_password` VARCHAR(255) NULL';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- temporary_password_expires_at: 임시 비밀번호 만료 시간
SET @col_name := 'temporary_password_expires_at';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `temporary_password_expires_at` DATETIME(6) NULL';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

