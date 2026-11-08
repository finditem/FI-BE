# ERD

## ERD 갱신

`dev`의 Flyway migration 적용 후 MySQL 테이블을 덤프해 dbdiagram에 반영합니다.

현재 저장소의 [DBML](../fmi-erd.dbml)은 ERD 자료입니다. 스키마 변경의 원본은 [Flyway migration](../../src/main/resources/db/migration)이며, DBML만 수정해 스키마를 변경하지 않습니다.
