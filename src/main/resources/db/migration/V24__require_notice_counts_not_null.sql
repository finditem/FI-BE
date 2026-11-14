-- 공지의 좋아요 수(like_count)와 조회수(view_cnt)는 항상 값이 있어야 하므로 NOT NULL, 기본값 0으로 둔다.
-- 원래 V1의 정의였으나 dev와 prod에는 반영되지 않았고, V1 보정(#643)에서 실제 DB 기준으로 NULL 허용으로 바뀌었다.
-- 증가 쿼리(count + 1)는 NULL이면 값이 늘지 않으므로 NULL을 허용하지 않는다. NULL 행이 있으면 이 migration은 실패한다.

ALTER TABLE `notice`
    MODIFY COLUMN `like_count` int NOT NULL DEFAULT 0,
    MODIFY COLUMN `view_cnt` int NOT NULL DEFAULT 0;
