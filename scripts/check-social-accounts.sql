-- 소셜 로그인 계정 확인 스크립트

-- 1. users 테이블 확인 (V12 마이그레이션으로 user -> users로 변경됨)
SELECT 
    id,
    email,
    nickname,
    phone_number,
    role,
    deleted_at,
    created_at,
    updated_at
FROM users
ORDER BY created_at DESC
LIMIT 10;

-- 2. social_accounts 테이블 확인
SELECT 
    sa.social_id,
    sa.user AS user_id,
    sa.provider,
    sa.provider_id,
    sa.created_at,
    u.email,
    u.nickname
FROM social_accounts sa
LEFT JOIN users u ON sa.user = u.id
ORDER BY sa.created_at DESC;

-- 3. 카카오 계정만 확인
SELECT 
    sa.social_id,
    sa.user AS user_id,
    sa.provider,
    sa.provider_id,
    sa.created_at,
    u.email,
    u.nickname,
    u.created_at AS user_created_at
FROM social_accounts sa
LEFT JOIN users u ON sa.user = u.id
WHERE sa.provider = 'KAKAO'
ORDER BY sa.created_at DESC;

-- 4. 테이블 존재 여부 확인
SHOW TABLES LIKE 'users';
SHOW TABLES LIKE 'social_accounts';
SHOW TABLES LIKE 'user';  -- V12 이전 테이블명

-- 5. social_accounts 테이블 구조 확인
DESCRIBE social_accounts;

-- 6. users 테이블 구조 확인
DESCRIBE users;