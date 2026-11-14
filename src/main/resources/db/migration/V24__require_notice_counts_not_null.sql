ALTER TABLE `notice`
    MODIFY COLUMN `like_count` int NOT NULL DEFAULT 0,
    MODIFY COLUMN `view_cnt` int NOT NULL DEFAULT 0;
