-- 닉네임 UNIQUE 제약조건 추가
-- 기존 중복 데이터가 있을 수 있으므로 먼저 중복 제거 필요 (가장 오래된 것만 유지)

-- 1. 중복된 닉네임 처리 (활성 사용자만 체크, 가장 오래된 것 제외하고 나머지에 ID 추가)
UPDATE users u1
INNER JOIN (
    SELECT id, nickname, 
           ROW_NUMBER() OVER (PARTITION BY nickname ORDER BY created_at ASC, id ASC) as rn
    FROM users
    WHERE deleted_at IS NULL
) u2 ON u1.id = u2.id
SET u1.nickname = CONCAT(u1.nickname, '_', u1.id)
WHERE u2.rn > 1 AND u1.deleted_at IS NULL;

-- 2. UNIQUE 제약조건 추가
ALTER TABLE users
ADD UNIQUE KEY uk_users_nickname (nickname);
