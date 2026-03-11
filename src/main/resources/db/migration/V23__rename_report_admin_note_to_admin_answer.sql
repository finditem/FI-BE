-- report 테이블: admin_note 컬럼을 admin_answer로 변경
ALTER TABLE report CHANGE COLUMN admin_note admin_answer TEXT COMMENT '관리자 답변';
