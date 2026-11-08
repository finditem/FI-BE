ALTER TABLE `place_operation_period`
    ADD COLUMN `closing_at` datetime(6) NULL AFTER `end_date`;
