-- 공지사항 좋아요, 댓글 좋아요, 이미지 테이블 생성 및 컬럼 추가

-- notice 테이블에 like_count, draft 컬럼 추가
ALTER TABLE notice ADD COLUMN like_count INT DEFAULT 0 AFTER view_cnt;
ALTER TABLE notice ADD COLUMN draft BOOLEAN DEFAULT false AFTER like_count;

-- notice_comment 테이블에 like_count 컬럼 추가
ALTER TABLE notice_comment ADD COLUMN like_count INT DEFAULT 0 AFTER content;

-- report 테이블에 answered 컬럼 추가
ALTER TABLE report ADD COLUMN answered BOOLEAN NOT NULL DEFAULT false AFTER admin_note;

-- 공지사항 좋아요 테이블
CREATE TABLE notice_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notice_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notice_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_like_notice FOREIGN KEY (notice_id) REFERENCES notice(id) ON DELETE CASCADE,
    CONSTRAINT uk_notice_like_user_notice UNIQUE (user_id, notice_id),
    INDEX idx_notice_like_notice (notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 공지사항 댓글 좋아요 테이블
CREATE TABLE notice_comment_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notice_comment_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_comment_like_comment FOREIGN KEY (comment_id) REFERENCES notice_comment(comment_id) ON DELETE CASCADE,
    CONSTRAINT uk_notice_comment_like_user_comment UNIQUE (user_id, comment_id),
    INDEX idx_notice_comment_like_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 공지사항 이미지 테이블
CREATE TABLE notice_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    img_url VARCHAR(500) NOT NULL,
    image_type VARCHAR(20) NOT NULL,
    notice_id BIGINT NOT NULL,
    CONSTRAINT fk_notice_image_notice FOREIGN KEY (notice_id) REFERENCES notice(id) ON DELETE CASCADE,
    INDEX idx_notice_image_notice (notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
