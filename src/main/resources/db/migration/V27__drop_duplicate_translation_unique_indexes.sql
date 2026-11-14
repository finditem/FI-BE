SET @finditem_post_translation_dup_index = (
    SELECT MIN(duplicate.index_name)
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'post_translation'
          AND non_unique = 0
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'post_id,language_code'
    ) AS duplicate
    WHERE duplicate.index_name <> 'uk_post_translation'
      AND EXISTS (
          SELECT 1
          FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'post_translation'
            AND index_name = 'uk_post_translation'
      )
);

SET @finditem_post_translation_dup_ddl = IF(
    @finditem_post_translation_dup_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE post_translation DROP INDEX `', @finditem_post_translation_dup_index, '`')
);

PREPARE finditem_post_translation_dup_statement FROM @finditem_post_translation_dup_ddl;
EXECUTE finditem_post_translation_dup_statement;
DEALLOCATE PREPARE finditem_post_translation_dup_statement;

SET @finditem_place_translation_dup_index = (
    SELECT MIN(duplicate.index_name)
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'place_translation'
          AND non_unique = 0
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'place_id,language_code'
    ) AS duplicate
    WHERE duplicate.index_name <> 'uk_place_translation'
      AND EXISTS (
          SELECT 1
          FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'place_translation'
            AND index_name = 'uk_place_translation'
      )
);

SET @finditem_place_translation_dup_ddl = IF(
    @finditem_place_translation_dup_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE place_translation DROP INDEX `', @finditem_place_translation_dup_index, '`')
);

PREPARE finditem_place_translation_dup_statement FROM @finditem_place_translation_dup_ddl;
EXECUTE finditem_place_translation_dup_statement;
DEALLOCATE PREPARE finditem_place_translation_dup_statement;
