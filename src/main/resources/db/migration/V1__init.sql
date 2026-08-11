-- FMI 전체 스키마 초기화 (운영 DB 기준 통합)

CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `email_verified` bit(1) NOT NULL DEFAULT b'0',
  `marketing_consent` bit(1) NOT NULL,
  `nickname` varchar(255) NOT NULL,
  `original_password` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `privacy_policy_agreed` bit(1) NOT NULL,
  `terms_of_service_agreed` bit(1) NOT NULL DEFAULT b'0',
  `content_policy_agreed` bit(1) NOT NULL DEFAULT b'0',
  `profile_img` varchar(1000) NOT NULL DEFAULT '',
  `role` enum('ADMIN','USER') NOT NULL,
  `temporary_password` varchar(255) DEFAULT NULL,
  `temporary_password_expires_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `withdrawal_other_reason` text,
  `withdrawal_reason` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname`),
  UNIQUE KEY `UKob8kqyqqgmefl0aco34akdtpe` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `blocked_user` (
  `blocked_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `blocked_user_id` bigint NOT NULL,
  `blocker_user_id` bigint NOT NULL,
  PRIMARY KEY (`blocked_id`),
  UNIQUE KEY `uk_blocker_blocked` (`blocker_user_id`,`blocked_user_id`),
  KEY `FK73ruetkswycffqrc7l4sf4k9w` (`blocked_user_id`),
  CONSTRAINT `FK73ruetkswycffqrc7l4sf4k9w` FOREIGN KEY (`blocked_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKr7j3klrhd3ayf4ibsb9e9kga7` FOREIGN KEY (`blocker_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) NOT NULL,
  `category` enum('BAG','CARD','ELECTRONICS','ETC','ID_CARD','JEWELRY','WALLET') DEFAULT NULL,
  `content` varchar(500) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `date` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `item_status` enum('FOUND','SEARCHING') DEFAULT NULL,
  `post_type` enum('FOUND','LOST') DEFAULT NULL,
  `radius` int DEFAULT NULL,
  `temporary_save` bit(1) NOT NULL,
  `title` varchar(50) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `view_cnt` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7ky67sgi7k0ayf22652f7763r` (`user_id`),
  CONSTRAINT `FK7ky67sgi7k0ayf22652f7763r` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `post_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_type` enum('NORMAL','THUMBNAIL') NOT NULL,
  `img_url` varchar(255) NOT NULL,
  `post_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsip7qv57jw2fw50g97t16nrjr` (`post_id`),
  CONSTRAINT `FKsip7qv57jw2fw50g97t16nrjr` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `post_favorite` (
  `favorite_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_favorite` bit(1) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `UKc8dgrrihj0hpbg6ftse1vdu9b` (`user_id`,`post_id`),
  KEY `FKtnr54tuktg3welr2u950p0mqr` (`post_id`),
  CONSTRAINT `FK937yqqte113cikxpdcub6jhsp` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKtnr54tuktg3welr2u950p0mqr` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `comment` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted` bit(1) NOT NULL,
  `depth` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `FKde3rfu96lep00br5ov0mdieyt` (`parent_id`),
  KEY `FKs1slvnkuemjsq2kj4h3vhx7i1` (`post_id`),
  KEY `FKqm52p1v3o13hy268he0wcngr5` (`user_id`),
  CONSTRAINT `FKde3rfu96lep00br5ov0mdieyt` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`comment_id`),
  CONSTRAINT `FKqm52p1v3o13hy268he0wcngr5` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKs1slvnkuemjsq2kj4h3vhx7i1` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `comment_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `img_url` varchar(255) NOT NULL,
  `comment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKs1et2hjm1b13mvs63acb8wgi8` (`comment_id`),
  CONSTRAINT `FKs1et2hjm1b13mvs63acb8wgi8` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `is_like` bit(1) DEFAULT NULL,
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo8urnlomri10dub10y8ket23c` (`user_id`,`comment_id`),
  KEY `FKqlv8phl1ibeh0efv4dbn3720p` (`comment_id`),
  CONSTRAINT `FKl5wrmp8eoy5uegdo3473jqqi` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKqlv8phl1ibeh0efv4dbn3720p` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `chat_room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `post_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdedif34f1oocp49p9lxh3tglc` (`post_id`),
  CONSTRAINT `FKdedif34f1oocp49p9lxh3tglc` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `message_type` enum('IMAGE','TEXT') DEFAULT NULL,
  `readornot` bit(1) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `chat_room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj52yap2xrm9u0721dct0tjor9` (`chat_room_id`),
  KEY `FKd3oo2bhroe16913kh5pwtgk3h` (`user_id`),
  CONSTRAINT `FKd3oo2bhroe16913kh5pwtgk3h` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKj52yap2xrm9u0721dct0tjor9` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `chat_room_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `first_unread_at` datetime(6) DEFAULT NULL,
  `last_message_sent_at` datetime(6) DEFAULT NULL,
  `last_read_message_id` bigint DEFAULT NULL,
  `last_reminded_at` datetime(6) DEFAULT NULL,
  `participant_state` enum('ACTIVE','LEFT') DEFAULT NULL,
  `unread_count` bigint DEFAULT NULL,
  `visible_from_message_id` bigint DEFAULT NULL,
  `chat_room_id` bigint DEFAULT NULL,
  `last_message_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7h6dw4bfuokc7aejbqo9rbwou` (`chat_room_id`),
  KEY `FKlyv8xi1lnbbry44iamvx1l4re` (`last_message_id`),
  KEY `FKll38sgo5ugtwke3yj395sas9c` (`user_id`),
  CONSTRAINT `FK7h6dw4bfuokc7aejbqo9rbwou` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`),
  CONSTRAINT `FKll38sgo5ugtwke3yj395sas9c` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKlyv8xi1lnbbry44iamvx1l4re` FOREIGN KEY (`last_message_id`) REFERENCES `chat_message` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `message_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(1000) NOT NULL,
  `chat_message_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKaet3f72to9k3dof8de9vfdcn5` (`chat_message_id`),
  CONSTRAINT `FKaet3f72to9k3dof8de9vfdcn5` FOREIGN KEY (`chat_message_id`) REFERENCES `chat_message` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `customer_inquiry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answer_status` enum('ANSWERED','PENDING','RECEIVED') NOT NULL,
  `answered` bit(1) NOT NULL DEFAULT b'0',
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `inquiry_type` enum('ACCOUNT_LOGIN','BUG','ETC','SUGGESTION','USAGE') NOT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtqthxutk92qe91lkk7ji7tnqv` (`user_id`),
  CONSTRAINT `FKtqthxutk92qe91lkk7ji7tnqv` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `inquiry_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `img_url` varchar(255) NOT NULL,
  `inquiry_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj41yg27mmk4n03evfx4c222el` (`inquiry_id`),
  CONSTRAINT `FKj41yg27mmk4n03evfx4c222el` FOREIGN KEY (`inquiry_id`) REFERENCES `customer_inquiry` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `inquiry_comment` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `inquiry_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `FK322hsdo1ygjamttbog4rwt8d5` (`inquiry_id`),
  KEY `FKg11nxgniivvpj6w1b5vtjjk8h` (`parent_id`),
  KEY `FKmeah8o8y42g3qyv7cho5c2s0` (`user_id`),
  CONSTRAINT `FK322hsdo1ygjamttbog4rwt8d5` FOREIGN KEY (`inquiry_id`) REFERENCES `customer_inquiry` (`id`),
  CONSTRAINT `FKg11nxgniivvpj6w1b5vtjjk8h` FOREIGN KEY (`parent_id`) REFERENCES `inquiry_comment` (`comment_id`),
  CONSTRAINT `FKmeah8o8y42g3qyv7cho5c2s0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `inquiry_comment_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `img_url` varchar(255) NOT NULL,
  `comment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK68eyt7t3n43dagrhgq3le7vkm` (`comment_id`),
  CONSTRAINT `FK68eyt7t3n43dagrhgq3le7vkm` FOREIGN KEY (`comment_id`) REFERENCES `inquiry_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` enum('EVENT','GENERAL','IMPORTANT','MAINTENANCE','UPDATE') DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `draft` bit(1) NOT NULL DEFAULT b'0',
  `like_count` int NOT NULL DEFAULT 0,
  `pinned` bit(1) NOT NULL DEFAULT b'0',
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `view_cnt` int NOT NULL DEFAULT 0,
  `author_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgib4imoxg1fihk96xdsgmu51o` (`author_id`),
  CONSTRAINT `FKgib4imoxg1fihk96xdsgmu51o` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notice_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_type` enum('NORMAL','THUMBNAIL') NOT NULL,
  `img_url` varchar(255) NOT NULL,
  `notice_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK13h2d99jqpw7xnaj5w1crvv8m` (`notice_id`),
  CONSTRAINT `FK13h2d99jqpw7xnaj5w1crvv8m` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notice_comment` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted` bit(1) NOT NULL,
  `like_count` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `notice_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `FKax7qcww5twiq84y8dvvpaketx` (`notice_id`),
  KEY `FK2dp7snc41wwep5tjhmeov0dl2` (`parent_id`),
  KEY `FK6ejye0l2wevu2cwcdodeif716` (`user_id`),
  CONSTRAINT `FK2dp7snc41wwep5tjhmeov0dl2` FOREIGN KEY (`parent_id`) REFERENCES `notice_comment` (`comment_id`),
  CONSTRAINT `FK6ejye0l2wevu2cwcdodeif716` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKax7qcww5twiq84y8dvvpaketx` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notice_comment_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `img_url` varchar(255) NOT NULL,
  `comment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKq4x4n48rkf8ou3j4cmoc0cvim` (`comment_id`),
  CONSTRAINT `FKq4x4n48rkf8ou3j4cmoc0cvim` FOREIGN KEY (`comment_id`) REFERENCES `notice_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notice_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK73ol3tai6n22b2q2dib9jpl0v` (`user_id`,`comment_id`),
  KEY `FKay1k1d1yfa3bnkexy6fs1yder` (`comment_id`),
  CONSTRAINT `FKay1k1d1yfa3bnkexy6fs1yder` FOREIGN KEY (`comment_id`) REFERENCES `notice_comment` (`comment_id`),
  CONSTRAINT `FKpcog6p2gruan4ccndsqw6a1iy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notice_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `notice_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr04bbm2ojofgchqjersn66ug4` (`user_id`,`notice_id`),
  KEY `FKm2krt4h5dcpydbb4tlvxyr27` (`notice_id`),
  CONSTRAINT `FK7n2njq5ihwop4jncobic43byd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKm2krt4h5dcpydbb4tlvxyr27` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notification` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `message` varchar(500) DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reference_type` enum('CHAT','COMMENT','INQUIRY','NOTICE','POST','REPORT') DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `type` enum('CATEGORY','CHAT','CHAT_REMINDER','COMMENT','FAVORITE','INQUIRY_REPLY','NOTICE','REPORT_RESULT','SYSTEM') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `FKnk4ftb5am9ubmkv1661h15ds9` (`user_id`),
  CONSTRAINT `FKnk4ftb5am9ubmkv1661h15ds9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notification_settings` (
  `settings_id` bigint NOT NULL AUTO_INCREMENT,
  `browser_notification_enabled` bit(1) DEFAULT b'1',
  `category_enabled` bit(1) DEFAULT b'1',
  `chat_enabled` bit(1) DEFAULT b'1',
  `comment_enabled` bit(1) DEFAULT b'1',
  `created_at` datetime(6) NOT NULL,
  `favorite_enabled` bit(1) DEFAULT b'1',
  `inquiry_reply_enabled` bit(1) DEFAULT b'1',
  `notice_enabled` bit(1) DEFAULT b'1',
  `report_result_enabled` bit(1) DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`settings_id`),
  UNIQUE KEY `UKm9ggfvif86mvq5382j88cequn` (`user_id`),
  CONSTRAINT `FKmh6alfw96lc851ea0snhijfk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notification_category_keywords` (
  `settings_id` bigint NOT NULL,
  `category` enum('BAG','CARD','ELECTRONICS','ETC','ID_CARD','JEWELRY','WALLET') DEFAULT NULL,
  KEY `FKid0vcqwads9b74n3xunbmhtsb` (`settings_id`),
  CONSTRAINT `FKid0vcqwads9b74n3xunbmhtsb` FOREIGN KEY (`settings_id`) REFERENCES `notification_settings` (`settings_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `report` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `admin_answer` text,
  `answered` bit(1) NOT NULL DEFAULT b'0',
  `answered_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `reason` text,
  `report_type` enum('DUPLICATE','ETC','EXTORTION','FALSE_CLAIM','IRRELEVANT_CONTENT','OFFENSIVE_LANGUAGE','SPAM') NOT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','RESOLVED','REVIEWED') NOT NULL,
  `target_id` bigint NOT NULL,
  `target_type` enum('CHAT','COMMENT','POST','USER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `admin_id` bigint DEFAULT NULL,
  `reporter_user_id` bigint NOT NULL,
  PRIMARY KEY (`report_id`),
  KEY `FKpj3e54p373onw0qyq8mtav170` (`admin_id`),
  KEY `FKrycrc7sl64bwyfdrr3rvvlacr` (`reporter_user_id`),
  CONSTRAINT `FKpj3e54p373onw0qyq8mtav170` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKrycrc7sl64bwyfdrr3rvvlacr` FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `report_answer_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `report_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKme3t6whfuybjgvq12k609u4bh` (`report_id`),
  CONSTRAINT `FKme3t6whfuybjgvq12k609u4bh` FOREIGN KEY (`report_id`) REFERENCES `report` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `social_accounts` (
  `social_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `provider` enum('GOOGLE','KAKAO','NAVER') NOT NULL,
  `provider_id` varchar(255) NOT NULL,
  `user` bigint NOT NULL,
  PRIMARY KEY (`social_id`),
  KEY `FK7oiebt63m99hgpr39atm0bvjv` (`user`),
  CONSTRAINT `FK7oiebt63m99hgpr39atm0bvjv` FOREIGN KEY (`user`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_category` (
  `user_category_id` bigint NOT NULL AUTO_INCREMENT,
  `category` enum('BAG','CARD','ELECTRONICS','ETC','ID_CARD','JEWELRY','WALLET') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_category_id`),
  UNIQUE KEY `uk_user_category` (`user_id`,`category`),
  CONSTRAINT `FKh1fip9lpe4alrpurxqfmpftvl` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ip_blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `ip` varchar(45) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgrg4if1re8s6hk4pitsdjrgpq` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- FULLTEXT 인덱스 (ngram parser, 한국어 검색 지원)
ALTER TABLE notice ADD FULLTEXT INDEX ft_notice_title_content (title, content) WITH PARSER ngram;
ALTER TABLE customer_inquiry ADD FULLTEXT INDEX ft_inquiry_title_content (title, content) WITH PARSER ngram;
ALTER TABLE report ADD FULLTEXT INDEX ft_report_reason (reason) WITH PARSER ngram;
