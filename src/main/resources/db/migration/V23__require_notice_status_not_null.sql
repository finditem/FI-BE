-- 공지의 임시저장(draft)과 고정(pinned)은 항상 값이 있어야 하므로 NOT NULL, 기본값 0으로 둔다.
-- 원래 V1의 정의였으나 dev와 prod에는 반영되지 않았고, V1 보정(#643)에서 실제 DB 기준으로 NULL 허용으로 바뀌었다.
-- 사용자 조회가 draft = false 조건을 쓰므로 draft가 NULL인 행은 노출되지 않는다.
-- NULL 행을 임의로 채우면 숨겨진 공지가 공개될 수 있어 데이터는 변경하지 않는다. NULL 행이 있으면 이 migration은 실패한다.

ALTER TABLE `notice`
    MODIFY COLUMN `draft` bit(1) NOT NULL DEFAULT b'0',
    MODIFY COLUMN `pinned` bit(1) NOT NULL DEFAULT b'0';
