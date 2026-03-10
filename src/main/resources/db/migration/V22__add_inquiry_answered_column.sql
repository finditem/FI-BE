ALTER TABLE customer_inquiry ADD COLUMN answered TINYINT(1) NOT NULL DEFAULT 0;

UPDATE customer_inquiry SET answered = 1 WHERE answer_status = 'ANSWERED';
