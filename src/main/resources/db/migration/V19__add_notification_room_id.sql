-- 개발 환경의 ddl-auto=update가 이미 생성한 컬럼도 허용한다.
SET @finditem_notification_room_id_ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE notification ADD COLUMN room_id BIGINT NULL',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification'
      AND column_name = 'room_id'
);

PREPARE finditem_notification_room_id_statement FROM @finditem_notification_room_id_ddl;
EXECUTE finditem_notification_room_id_statement;
DEALLOCATE PREPARE finditem_notification_room_id_statement;
