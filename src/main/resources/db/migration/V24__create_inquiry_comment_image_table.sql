-- 문의 댓글 이미지 테이블 생성
CREATE TABLE IF NOT EXISTS inquiry_comment_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    img_url VARCHAR(500) NOT NULL,
    comment_id BIGINT NOT NULL,
    FOREIGN KEY (comment_id) REFERENCES inquiry_comment(comment_id) ON DELETE CASCADE
);
