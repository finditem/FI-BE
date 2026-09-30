# 버그 수정

버그의 관찰 결과와 기대 결과를 구분하고, 같은 실패를 재현한 뒤 수정한다. 기대 결과가 정책서에 없으면 먼저 결정권자에게 묻는다.

## 1. 재현 조건을 고정한다

```text
입력: 어떤 사용자, 상태, 시각과 요청에서 발생했는가?
관찰: HTTP 결과, DB 상태, 외부 효과는 무엇이었는가?
기대: 어느 확정 정책에 따라 무엇이어야 하는가?
경계: 동시 요청, 재시도, 실패한 외부 호출이 있었는가?
```

코드의 현재 결과를 기대 결과로 해석하지 않는다. 정책과 구현이 다르면 차이를 보고한다.

## 2. 실패 지점까지 흐름을 따라간다

예를 들어 게시글 조회 문제는 Controller의 입력, Service의 검증, Repository 또는 QueryDSL의 조건, MySQL 결과 순서로 확인한다. QueryDSL의 조건과 Entity 정책이 겹치면 [Persistence](../architecture/persistence.md)의 일치성 기준을 적용한다.

## 3. 회귀 테스트를 선택한다

순수 정책 오류는 단위 테스트, JPA 조회와 transaction 오류는 MySQL Testcontainers 통합 테스트, 외부 HTTP 변환 오류는 WireMock 테스트를 사용한다. Web 테스트 범위는 [Testing](testing.md)을 따른다. 핵심 결과 한 가지를 명확하게 검증하고 수정 전후의 차이를 설명한다.

수정으로 오류 코드나 실패 경계가 바뀌면 [Error Handling](error-handling.md)을 확인한다. 적용 환경과 데이터 수정이 필요하면 [Deployment](../operations/deployment.md)를 함께 읽는다.
