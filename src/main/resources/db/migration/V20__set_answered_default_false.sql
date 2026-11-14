ALTER TABLE `customer_inquiry`
    ALTER COLUMN `answered` SET DEFAULT b'0';

ALTER TABLE `report`
    ALTER COLUMN `answered` SET DEFAULT b'0';
