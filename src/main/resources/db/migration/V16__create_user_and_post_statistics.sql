CREATE TABLE user_statistics (
 statistics_date DATE NOT NULL PRIMARY KEY,
 started_at DATETIME(6) NULL,
 opening_user_count BIGINT NOT NULL DEFAULT 0,
 signup_count BIGINT NOT NULL DEFAULT 0,
 withdrawal_count BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_user_statistics_started_at (started_at)
);
CREATE TABLE post_statistics (
 statistics_date DATE NOT NULL PRIMARY KEY,
 started_at DATETIME(6) NULL,
 opening_post_count BIGINT NOT NULL DEFAULT 0,
 lost_post_count BIGINT NOT NULL DEFAULT 0,
 found_post_count BIGINT NOT NULL DEFAULT 0,
 removed_count BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_post_statistics_started_at (started_at)
);
