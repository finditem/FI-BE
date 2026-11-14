-- 소셜 로그인이 제공하는 프로필 이미지 URL은 255자를 넘을 수 있으므로 users.profile_img를 varchar(1000)으로 확장한다.
-- 원래 V1의 정의였으나 dev와 prod에는 반영되지 않았고, V1 보정(#643)에서 실제 DB 기준으로 255로 바뀌었다.
-- 이미지 없음은 NULL과 빈 문자열을 모두 허용하며, 읽는 쪽은 둘을 같은 상태로 처리한다. NULL 허용과 데이터는 그대로 둔다.

ALTER TABLE `users`
    MODIFY COLUMN `profile_img` varchar(1000) DEFAULT NULL;
