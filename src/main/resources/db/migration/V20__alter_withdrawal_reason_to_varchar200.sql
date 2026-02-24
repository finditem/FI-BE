-- 탈퇴 사유 다중 선택 지원 (최대 3개, 콤마 구분 저장)
ALTER TABLE users MODIFY COLUMN withdrawal_reason VARCHAR(200);
