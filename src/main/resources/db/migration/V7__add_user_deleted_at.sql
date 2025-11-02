-- User 테이블에 deleted_at 컬럼 추가 (소프트 삭제용)
SET @col_name := 'deleted_at';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `deleted_at` DATETIME(6) NULL';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

