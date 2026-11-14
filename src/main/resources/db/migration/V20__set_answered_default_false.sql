-- 문의와 신고는 접수 직후 미답변 상태이므로 answered 기본값을 0으로 둔다.
-- 원래 V1의 정의였으나 dev와 prod에는 반영되지 않았고, V1 보정(#643)에서 실제 DB 기준으로 제거되었다.

ALTER TABLE `customer_inquiry`
    ALTER COLUMN `answered` SET DEFAULT b'0';

ALTER TABLE `report`
    ALTER COLUMN `answered` SET DEFAULT b'0';
