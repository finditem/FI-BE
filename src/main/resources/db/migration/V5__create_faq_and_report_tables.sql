-- 문의 답변 테이블 생성
CREATE TABLE IF NOT EXISTS inquiry_reply (
    reply_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_id BIGINT NOT NULL,
    admin_id BIGINT COMMENT '관리자 ID',
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    FOREIGN KEY (inquiry_id) REFERENCES customer_inquiry(id) ON DELETE CASCADE,
    INDEX idx_inquiry_id (inquiry_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 신고 테이블 생성
CREATE TABLE IF NOT EXISTS report (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL COMMENT '신고자',
    target_type VARCHAR(50) NOT NULL COMMENT 'POST, COMMENT, USER, CHAT',
    target_id BIGINT NOT NULL COMMENT '신고 대상 ID',
    report_type VARCHAR(50) NOT NULL COMMENT 'FRAUD, SPAM, INAPPROPRIATE, ABUSE, etc',
    reason TEXT NOT NULL COMMENT '신고 사유',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, REVIEWED, RESOLVED, REJECTED',
    admin_note TEXT COMMENT '관리자 메모',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    resolved_at DATETIME(6) NULL COMMENT '처리 완료일',
    FOREIGN KEY (reporter_user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    INDEX idx_reporter (reporter_user_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at DESC),
    UNIQUE KEY uk_reporter_target (reporter_user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notice 테이블에 category 컬럼 추가 (없는 경우에만)
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notice'
      AND COLUMN_NAME = 'category'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE notice ADD COLUMN category VARCHAR(50) DEFAULT ''GENERAL'' COMMENT ''GENERAL, EVENT, MAINTENANCE, IMPORTANT, UPDATE'' AFTER content',
    'SELECT ''Column already exists'' AS msg');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- CustomerInquiry 테이블에 category, email 컬럼 추가 (없는 경우에만)
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'customer_inquiry'
      AND COLUMN_NAME = 'category'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE customer_inquiry ADD COLUMN category VARCHAR(50) COMMENT ''GENERAL, TECHNICAL, ACCOUNT, PAYMENT, REPORT_ISSUE, SERVICE, ETC'' AFTER inquiry_type',
    'SELECT ''Column already exists'' AS msg');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'customer_inquiry'
      AND COLUMN_NAME = 'email'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE customer_inquiry ADD COLUMN email VARCHAR(255) COMMENT ''비회원 문의 시 이메일'' AFTER answer_status',
    'SELECT ''Column already exists'' AS msg');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 알림 설정 테이블 생성
CREATE TABLE IF NOT EXISTS notification_settings (
    settings_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    comment_enabled BOOLEAN DEFAULT TRUE COMMENT '댓글 알림',
    chat_enabled BOOLEAN DEFAULT TRUE COMMENT '채팅 알림',
    inquiry_reply_enabled BOOLEAN DEFAULT TRUE COMMENT '문의 답변 알림',
    report_result_enabled BOOLEAN DEFAULT TRUE COMMENT '신고 처리 결과 알림',
    favorite_enabled BOOLEAN DEFAULT TRUE COMMENT '좋아요 알림',
    notice_enabled BOOLEAN DEFAULT TRUE COMMENT '공지사항 알림',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification 테이블에 컬럼 추가 (없는 경우에만)
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND COLUMN_NAME = 'title'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE notification ADD COLUMN title VARCHAR(200) NOT NULL DEFAULT ''알림'' AFTER type',
    'SELECT ''Column already exists'' AS msg');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND COLUMN_NAME = 'reference_type'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE notification ADD COLUMN reference_type VARCHAR(50) AFTER message',
    'SELECT ''Column already exists'' AS msg');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND COLUMN_NAME = 'reference_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE notification ADD COLUMN reference_id BIGINT AFTER reference_type',
    'SELECT ''Column already exists'' AS msg');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 type 컬럼 크기 조정
ALTER TABLE notification MODIFY COLUMN type VARCHAR(50);

-- 초기 데이터 삽입 (Notice) - 이미 데이터가 있을 수 있으므로 중복 체크
INSERT INTO notice (title, content, category, pinned, view_cnt, created_at, updated_at)
SELECT * FROM (SELECT '서비스 이용 안내' AS title, '분실물/습득물 서비스를 이용해주셔서 감사합니다. 안전한 거래를 위해 신뢰할 수 있는 정보를 등록해주세요.' AS content, 'IMPORTANT' AS category, 1 AS pinned, 0 AS view_cnt, NOW() AS created_at, NOW() AS updated_at) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM notice WHERE title = '서비스 이용 안내'
) LIMIT 1;

