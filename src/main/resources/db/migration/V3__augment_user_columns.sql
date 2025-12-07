-- Augment `user` table to match JPA entity fields (idempotent)

-- helper to add column if missing
-- usage: set @col_name and @ddl then execute prepared statement conditionally

-- name column removed in V11

-- email_verified TINYINT(1) NOT NULL DEFAULT 0
SET @col_name := 'email_verified';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `email_verified` TINYINT(1) NOT NULL DEFAULT 0';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- phone_verified TINYINT(1) NOT NULL DEFAULT 0
SET @col_name := 'phone_verified';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `phone_verified` TINYINT(1) NOT NULL DEFAULT 0';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- role VARCHAR(50) NOT NULL DEFAULT 'USER'
SET @col_name := 'role';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `role` VARCHAR(50) NOT NULL DEFAULT ''USER''';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- profile_img VARCHAR(1000) NOT NULL DEFAULT ''
SET @col_name := 'profile_img';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := "ALTER TABLE `user` ADD COLUMN `profile_img` VARCHAR(1000) NOT NULL DEFAULT ''";
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- updated_at DATETIME(6) NULL
SET @col_name := 'updated_at';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `updated_at` DATETIME(6) NULL';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- trust_score BIGINT NULL DEFAULT 0
SET @col_name := 'trust_score';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `trust_score` BIGINT NULL DEFAULT 0';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- termsOfServiceAgreed TINYINT(1) NOT NULL DEFAULT 0
SET @col_name := 'termsOfServiceAgreed';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `termsOfServiceAgreed` TINYINT(1) NOT NULL DEFAULT 0';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- privacyPolicyAgreed TINYINT(1) NOT NULL DEFAULT 0
SET @col_name := 'privacyPolicyAgreed';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `privacyPolicyAgreed` TINYINT(1) NOT NULL DEFAULT 0';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- marketingConsent TINYINT(1) NOT NULL DEFAULT 0
SET @col_name := 'marketingConsent';
SET @exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = @col_name);
SET @ddl := 'ALTER TABLE `user` ADD COLUMN `marketingConsent` TINYINT(1) NOT NULL DEFAULT 0';
SET @sql := IF(@exists = 0, @ddl, 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;


