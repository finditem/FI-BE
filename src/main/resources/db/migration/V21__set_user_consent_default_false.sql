-- 약관 동의 전에는 미동의 상태이므로 이용약관과 콘텐츠 활용 동의의 기본값을 0으로 둔다.
-- 원래 V1의 정의였으나 dev와 prod에는 반영되지 않았고, V1 보정(#643)에서 실제 DB 기준으로 제거되었다.

ALTER TABLE `users`
    ALTER COLUMN `terms_of_service_agreed` SET DEFAULT b'0',
    ALTER COLUMN `content_policy_agreed` SET DEFAULT b'0';
