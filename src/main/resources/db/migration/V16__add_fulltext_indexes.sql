-- FULLTEXT INDEX with ngram parser for Korean text search
-- MySQL 8.0+ ngram parser supports CJK characters (default token size: 2)

-- 공지사항 제목+내용 검색
ALTER TABLE notice ADD FULLTEXT INDEX ft_notice_title_content (title, content) WITH PARSER ngram;

-- 문의 제목+내용 검색
ALTER TABLE customer_inquiry ADD FULLTEXT INDEX ft_inquiry_title_content (title, content) WITH PARSER ngram;

-- 신고 사유 검색
ALTER TABLE report ADD FULLTEXT INDEX ft_report_reason (reason) WITH PARSER ngram;
