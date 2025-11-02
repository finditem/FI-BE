-- Post와 Comment 테이블의 user_id를 nullable로 변경하고 Foreign Key 제약조건 수정 (익명화 처리용)

-- Post 테이블의 user_id를 nullable로 변경
ALTER TABLE post 
MODIFY COLUMN user_id BIGINT NULL;

-- Post 테이블의 Foreign Key 제약조건 삭제 후 재생성 (ON DELETE SET NULL)
ALTER TABLE post 
DROP FOREIGN KEY fk_post_user;

ALTER TABLE post 
ADD CONSTRAINT fk_post_user 
FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE SET NULL;

-- Comment 테이블의 user_id를 nullable로 변경
ALTER TABLE comment 
MODIFY COLUMN user_id BIGINT NULL;

-- Comment 테이블의 Foreign Key 제약조건 삭제 후 재생성 (ON DELETE SET NULL)
ALTER TABLE comment 
DROP FOREIGN KEY fk_comment_user;

ALTER TABLE comment 
ADD CONSTRAINT fk_comment_user 
FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE SET NULL;

-- FavoritePost는 탈퇴 시 삭제하므로 변경하지 않음 (유지)

