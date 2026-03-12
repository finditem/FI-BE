-- report 테이블에 admin_id, answered_at 컬럼 추가
ALTER TABLE report ADD COLUMN admin_id BIGINT NULL;
ALTER TABLE report ADD COLUMN answered_at DATETIME(6) NULL;
ALTER TABLE report ADD CONSTRAINT fk_report_admin FOREIGN KEY (admin_id) REFERENCES `users`(id) ON DELETE SET NULL;

-- 신고 답변 이미지 테이블 생성
CREATE TABLE IF NOT EXISTS report_answer_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (report_id) REFERENCES report(report_id) ON DELETE CASCADE
);
