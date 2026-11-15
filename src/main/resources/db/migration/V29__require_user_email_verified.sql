ALTER TABLE `users`
    MODIFY COLUMN `email_verified` bit(1) NOT NULL DEFAULT b'0';
