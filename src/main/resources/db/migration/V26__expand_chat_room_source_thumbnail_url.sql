SET @finditem_chat_room_thumbnail_ddl = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE chat_room MODIFY COLUMN source_thumbnail_url varchar(512) NULL',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_room'
      AND column_name = 'source_thumbnail_url'
      AND character_maximum_length < 512
);

PREPARE finditem_chat_room_thumbnail_statement FROM @finditem_chat_room_thumbnail_ddl;
EXECUTE finditem_chat_room_thumbnail_statement;
DEALLOCATE PREPARE finditem_chat_room_thumbnail_statement;
