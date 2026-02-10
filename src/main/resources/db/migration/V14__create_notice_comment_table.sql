-- 공지사항 댓글 테이블 생성
CREATE TABLE IF NOT EXISTS notice_comment (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_notice_comment_notice FOREIGN KEY (notice_id) REFERENCES notice(id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_comment_parent FOREIGN KEY (parent_id) REFERENCES notice_comment(comment_id) ON DELETE CASCADE,
    INDEX idx_notice_comment_notice (notice_id),
    INDEX idx_notice_comment_parent (parent_id),
    INDEX idx_notice_comment_notice_cursor (notice_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
