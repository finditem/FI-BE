-- Create tables inferred from JPA entities that are not present in V1

-- chat_message
CREATE TABLE IF NOT EXISTS chat_message (
    chatmessage_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    readornot BIT(1) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_chat_message_user FOREIGN KEY (user_id) REFERENCES `user`(id)
);

-- post
CREATE TABLE IF NOT EXISTS post (
    post_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    view_cnt VARCHAR(255),
    item_status VARCHAR(50),
    post_type VARCHAR(50),
    content TEXT NOT NULL,
    p_content TEXT NOT NULL,
    updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES `user`(id)
);

-- comment
CREATE TABLE IF NOT EXISTS comment (
    comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    p_content VARCHAR(255) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES `user`(id)
);

-- favorite_post
CREATE TABLE IF NOT EXISTS favorite_post (
    favorite_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES `user`(id),
    CONSTRAINT fk_favorite_post FOREIGN KEY (post_id) REFERENCES post(post_id)
);

-- chat_room
CREATE TABLE IF NOT EXISTS chat_room (
    chatroom_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_chatroom_user FOREIGN KEY (user_id) REFERENCES `user`(id)
);

-- notice
CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    content TEXT,
    pinned BIT(1),
    view_cnt VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6)
);

-- post_image
CREATE TABLE IF NOT EXISTS post_image (
    postimage_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    img_url VARCHAR(1000) NOT NULL,
    displayOrder INT NOT NULL,
    post BIGINT NOT NULL,
    CONSTRAINT fk_postimage_post FOREIGN KEY (post) REFERENCES post(post_id)
);

-- user constraints/indexes (guarded for idempotency)
-- ensure missing column exists (MySQL may not support IF NOT EXISTS for ADD COLUMN)
SET @__col := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'phone_number'
);
SET @__sql := IF(@__col = 0,
                 'ALTER TABLE `user` ADD COLUMN `phone_number` VARCHAR(255) NULL',
                 'SELECT 1');
PREPARE __stmt FROM @__sql; EXECUTE __stmt; DEALLOCATE PREPARE __stmt;

-- email index
SET @__exists := (SELECT COUNT(1)
                  FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'user'
                    AND index_name = 'ux_user_email');
SET @__sql := IF(@__exists = 0,
                 'CREATE UNIQUE INDEX ux_user_email ON `user`(email)',
                 'SELECT 1');
PREPARE __stmt FROM @__sql; EXECUTE __stmt; DEALLOCATE PREPARE __stmt;

-- phone_number index
SET @__exists := (SELECT COUNT(1)
                  FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'user'
                    AND index_name = 'ux_user_phone_number');
SET @__sql := IF(@__exists = 0,
                 'CREATE UNIQUE INDEX ux_user_phone_number ON `user`(phone_number)',
                 'SELECT 1');
PREPARE __stmt FROM @__sql; EXECUTE __stmt; DEALLOCATE PREPARE __stmt;


