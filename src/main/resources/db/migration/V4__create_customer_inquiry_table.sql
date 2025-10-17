CREATE TABLE IF NOT EXISTS customer_inquiry (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  inquiry_type VARCHAR(30) NOT NULL,
  answer_status VARCHAR(30) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NULL,
  user_id BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_customer_inquiry_user
    FOREIGN KEY (user_id) REFERENCES `user`(id)
);