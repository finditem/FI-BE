# 버그 수정

버그의 관찰 결과와 기대 결과를 구분하고, 같은 실패를 재현한 뒤 수정합니다. 기대 결과가 정책서에 없으면 먼저 결정권자에게 묻습니다.

## 1. 재현 조건을 고정합니다

```text
입력: 어떤 사용자, 상태, 시각과 요청에서 발생했는가?
관찰: HTTP 결과, DB 상태, 외부 효과는 무엇이었는가?
기대: 어느 확정 정책에 따라 무엇이어야 하는가?
경계: 동시 요청, 재시도, 실패한 외부 호출이 있었는가?
```

코드의 현재 결과를 기대 결과로 해석하지 않습니다. 정책과 구현이 다르면 차이를 보고합니다.

## 2. 실패 지점까지 흐름을 따라갑니다

게시글 조회: `Controller 입력 → Service 검증 → Repository/QueryDSL 조건 → MySQL 결과`. QueryDSL의 조건과 Entity 정책이 겹치면 [Persistence](../architecture/persistence.md)의 일치성 기준을 적용합니다.

## 3. 회귀 테스트를 선택합니다

[Testing](testing.md)의 테스트 선택 기준에 따라 회귀 테스트를 작성하고 수정 전후의 핵심 결과를 검증합니다.

수정으로 오류 코드나 실패 경계가 바뀌면 [Error Handling](error-handling.md)을 확인합니다. 적용 환경과 데이터 수정이 필요하면 [Deployment](../operations/deployment.md)를 함께 읽습니다.
