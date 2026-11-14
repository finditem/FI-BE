ALTER TABLE `users`
    ALTER COLUMN `terms_of_service_agreed` SET DEFAULT b'0',
    ALTER COLUMN `content_policy_agreed` SET DEFAULT b'0';
