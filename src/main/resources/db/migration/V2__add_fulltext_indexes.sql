-- notice 테이블 FULLTEXT 인덱스 (검색 기능에서 MATCH AGAINST 사용)
ALTER TABLE notice ADD FULLTEXT INDEX ft_notice_title_content (title, content) WITH PARSER ngram;

-- customer_inquiry 테이블 FULLTEXT 인덱스
ALTER TABLE customer_inquiry ADD FULLTEXT INDEX ft_inquiry_title_content (title, content) WITH PARSER ngram;

-- report 테이블 FULLTEXT 인덱스
ALTER TABLE report ADD FULLTEXT INDEX ft_report_reason (reason) WITH PARSER ngram;
