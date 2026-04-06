-- chat_room 테이블에 게시글 스냅샷 컬럼 추가 및 post_id nullable 변경

ALTER TABLE `chat_room`
    MODIFY COLUMN `post_id` bigint NULL,
    ADD COLUMN `source_post_id`      bigint                                                              NULL,
    ADD COLUMN `source_title`        varchar(255)                                                        NULL,
    ADD COLUMN `source_address`      varchar(255)                                                        NULL,
    ADD COLUMN `source_post_type`    enum('LOST','FOUND')                                                NULL,
    ADD COLUMN `source_postStatus`   enum('SEARCHING','FOUND')                                           NULL,
    ADD COLUMN `source_category`     enum('ELECTRONICS','WALLET','ID_CARD','JEWELRY','BAG','CARD','ETC') NULL,
    ADD COLUMN `source_thumbnail_url` varchar(512)                                                       NULL,
    ADD COLUMN `source_post_deleted` bit(1)                                                              NOT NULL DEFAULT 0;
