# erd

## ERD 갱신 workflow

`dev` 환경 서버 기준 flyway 를 통해 migration 되어 mysql에 반영된 후 mysql에서 table 덤프를 통해 dbdiagram과 싱크합니다.

`dev` 환경 서버 기준입니다.

현재 저장소의 [DBML](../fmi-erd.dbml)은 ERD 자료다. 스키마 변경의 원본은 [Flyway migration](../../src/main/resources/db/migration)이며, DBML만 수정해 스키마를 변경하지 않는다.
