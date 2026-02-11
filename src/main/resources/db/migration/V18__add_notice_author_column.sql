-- 공지사항 작성자 컬럼 추가 (기존 공지는 NULL → "관리자"로 폴백)
ALTER TABLE notice ADD COLUMN author_id BIGINT NULL AFTER draft;
ALTER TABLE notice ADD CONSTRAINT fk_notice_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;
