-- FAQ 테이블 생성
CREATE TABLE IF NOT EXISTS faq (
    faq_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(300) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50) NOT NULL COMMENT 'USAGE, ACCOUNT, PAYMENT, REPORT, TECHNICAL, ETC',
    order_num INT DEFAULT 0 COMMENT '정렬 순서',
    view_count INT DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    INDEX idx_category_order (category, order_num),
    INDEX idx_view_count (view_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

-- 초기 데이터 삽입 (FAQ)
INSERT INTO faq (question, answer, category, order_num, created_at, updated_at) VALUES
('분실물은 어떻게 등록하나요?', '게시판에서 분실물 카테고리를 선택하고 상세 정보(물품명, 분실 장소, 분실 일시, 특징)를 작성하면 됩니다. 사진이 있다면 함께 첨부해주세요.', 'USAGE', 1, NOW(), NOW()),
('습득물을 찾았어요. 어떻게 하나요?', '습득물 게시판에 습득한 물품의 사진과 함께 습득 장소, 습득 일시를 등록해주세요. 주인이 찾아갈 수 있도록 상세히 작성해주시면 감사하겠습니다.', 'USAGE', 2, NOW(), NOW()),
('회원 탈퇴는 어떻게 하나요?', '마이페이지 > 설정 > 회원 탈퇴에서 가능합니다. 탈퇴 시 작성한 게시글과 댓글은 삭제되지 않으며, 개인정보만 삭제됩니다.', 'ACCOUNT', 1, NOW(), NOW()),
('비밀번호를 잊어버렸어요.', '로그인 화면에서 "비밀번호 찾기"를 클릭하여 가입한 이메일로 비밀번호 재설정 링크를 받으실 수 있습니다.', 'ACCOUNT', 2, NOW(), NOW()),
('신고는 어떻게 하나요?', '부적절한 게시글이나 댓글 우측의 신고 버튼을 클릭하여 신고 사유를 작성하시면 됩니다. 신고된 내용은 관리자가 검토 후 조치합니다.', 'REPORT', 1, NOW(), NOW());

-- 초기 데이터 삽입 (Notice) - 이미 데이터가 있을 수 있으므로 중복 체크
INSERT INTO notice (title, content, category, pinned, view_cnt, created_at, updated_at)
SELECT * FROM (SELECT '서비스 이용 안내' AS title, '분실물/습득물 서비스를 이용해주셔서 감사합니다. 안전한 거래를 위해 신뢰할 수 있는 정보를 등록해주세요.' AS content, 'IMPORTANT' AS category, 1 AS pinned, 0 AS view_cnt, NOW() AS created_at, NOW() AS updated_at) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM notice WHERE title = '서비스 이용 안내'
) LIMIT 1;

