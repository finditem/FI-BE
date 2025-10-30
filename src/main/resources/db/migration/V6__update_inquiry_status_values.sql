-- 문의 상태값 업데이트: PENDING -> RECEIVED, ANSWERED -> ANSWERED, CLOSED -> PENDING
-- 기존 데이터 마이그레이션
UPDATE customer_inquiry 
SET answer_status = 'RECEIVED' 
WHERE answer_status = 'PENDING';

UPDATE customer_inquiry 
SET answer_status = 'PENDING' 
WHERE answer_status = 'CLOSED';

-- answer_status 컬럼에 기본값 설정
ALTER TABLE customer_inquiry 
MODIFY COLUMN answer_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED';
