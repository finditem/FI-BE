-- 개발 환경의 ddl-auto=update가 Entity의 @UniqueConstraint로 추가 생성한 중복 UNIQUE 인덱스를 삭제한다.
-- V8, V14가 만든 정식 인덱스가 같은 컬럼 조합의 유일성을 보장하므로 정식 인덱스가 있을 때만 삭제한다. prod와 신규 DB에는 변경이 없다.

-- post_translation: post_id,language_code 컬럼 조합의 UNIQUE 인덱스 중 정식 이름(uk_post_translation)이 아닌 인덱스를 삭제한다.
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

-- place_translation: place_id,language_code 컬럼 조합의 UNIQUE 인덱스 중 정식 이름(uk_place_translation)이 아닌 인덱스를 삭제한다.
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
