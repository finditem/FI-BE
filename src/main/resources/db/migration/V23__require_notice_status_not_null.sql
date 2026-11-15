ALTER TABLE `notice`
    MODIFY COLUMN `draft` bit(1) NOT NULL DEFAULT b'0',
    MODIFY COLUMN `pinned` bit(1) NOT NULL DEFAULT b'0';
