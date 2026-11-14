ALTER TABLE `users`
    MODIFY COLUMN `email` varchar(255) NOT NULL;

SET @finditem_users_email_unique_ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE users ADD UNIQUE KEY UKob8kqyqqgmefl0aco34akdtpe (email)',
        'SELECT 1'
    )
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'users'
          AND non_unique = 0
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'email'
    ) AS email_unique
);

PREPARE finditem_users_email_unique_statement FROM @finditem_users_email_unique_ddl;
EXECUTE finditem_users_email_unique_statement;
DEALLOCATE PREPARE finditem_users_email_unique_statement;
