-- 관련 쓰기 요청, 배치와 모든 기존 트랜잭션을 중지한 뒤 단일 실행자로 실행합니다.
-- 운영 데이터베이스를 명시적으로 선택한 MySQL CLI에서 실행합니다.
-- 테이블 생성은 Flyway V16의 책임이며 이 파일은 자동 실행하지 않습니다.
DELIMITER $$
CREATE PROCEDURE initialize_statistics()
BEGIN
    DECLARE initialized BIGINT DEFAULT 0;
    DECLARE started DATETIME(6);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    SET time_zone = '+09:00';
    START TRANSACTION;
    SELECT (SELECT COUNT(*) FROM user_statistics)
         + (SELECT COUNT(*) FROM post_statistics) INTO initialized;
    IF initialized <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Statistics tables must both be empty';
    END IF;
    SET started = NOW(6);
    INSERT INTO user_statistics (statistics_date, started_at, opening_user_count)
    SELECT DATE(started), started, COUNT(*) FROM users
    WHERE role = 'USER' AND deleted_at IS NULL
      AND privacy_policy_agreed = TRUE AND terms_of_service_agreed = TRUE;
    INSERT INTO post_statistics (statistics_date, started_at, opening_post_count)
    SELECT DATE(started), started, COUNT(*) FROM post
    WHERE is_deleted = FALSE AND temporary_save = FALSE;
    COMMIT;
END$$
DELIMITER ;
CALL initialize_statistics();
DROP PROCEDURE initialize_statistics;
