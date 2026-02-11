-- InquiryCategory → InquiryType 통합: category 값을 inquiry_type으로 이관
-- 기존: inquiry_type = 'PRIVATE' (전체), category = 'ACCOUNT'|'GENERAL'|... (사용자 선택)
-- 변경: inquiry_type에 category 값 저장, PRIVATE 제거

-- category 값이 있으면 inquiry_type에 복사
UPDATE customer_inquiry
SET inquiry_type = category
WHERE category IS NOT NULL AND category != '';

-- category가 없거나 여전히 PRIVATE인 행은 GENERAL로 기본값
UPDATE customer_inquiry
SET inquiry_type = 'GENERAL'
WHERE inquiry_type = 'PRIVATE' OR inquiry_type IS NULL;
