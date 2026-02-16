-- 문의 댓글 테이블 생성
CREATE TABLE IF NOT EXISTS inquiry_comment (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_id BIGINT NOT NULL,
    user_id BIGINT NULL COMMENT '회원 댓글 작성자 ID',
    email VARCHAR(255) NULL COMMENT '비회원 댓글 작성자 이메일',
    content TEXT NOT NULL,
    parent_id BIGINT NULL COMMENT '대댓글인 경우 부모 댓글 ID',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    FOREIGN KEY (inquiry_id) REFERENCES customer_inquiry(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_id) REFERENCES inquiry_comment(comment_id) ON DELETE CASCADE,
    INDEX idx_inquiry_id (inquiry_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 inquiry_reply 테이블 삭제 (InquiryReply 엔티티 제거로 인해 더 이상 사용하지 않음)
DROP TABLE IF EXISTS inquiry_reply;
