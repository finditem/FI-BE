# Decision Guide

[Architecture Overview](overview.md)에서 전체 흐름을 확인한 뒤, 실제 요구가 생긴 위치에만 구조를 추가한다. 이 문서는 새 정책을 정하지 않는다. 정책이 없거나 팀 논의 대상이면 질문을 남긴다.

| 고민 | 먼저 볼 기준 | 결정 조건 |
| --- | --- | --- |
| 새 도메인 또는 `global` | [Domain Boundaries](domain-boundaries.md) | 데이터 owner와 변경 이유가 분명한가? 경계가 애매하면 팀과 논의한다. |
| `service/internal` 분리 | [Domain Boundaries](domain-boundaries.md) | 조회, 검증, 계산 등의 독립 책임과 재사용 필요가 실제로 있는가? 분리 시점은 팀과 논의한다. |
| JPA Repository, QueryDSL, native SQL | [Persistence](persistence.md) | 단순 저장인가, 목록과 집계인가, SQL이 더 분명한가? native SQL은 근거와 MySQL 테스트가 필요하다. |
| JPA 연관관계 | [Persistence](persistence.md) | 같은 Aggregate 안인가? 밖이라면 ID 참조가 기본이다. |
| DB 제약 | [Persistence](persistence.md) | 참조 무결성과 유일성을 동시 요청에서도 보장해야 하는가? |
| port와 외부 client | [Domain Boundaries](domain-boundaries.md) | 도입 기준은 팀 논의 대상이다. 공급자 API를 업무 규칙으로 끌어오지 않는다. |
| 오류 발생 위치 | [Error Handling](../contributing/error-handling.md) | 입력 형식, 업무 검증, 조회 부재, 인프라 실패 중 어디서 결정할 오류인가? |

## 변경 범위 기록

다음 표는 구현 전에 채울 수 있다. 모르는 업무 조건은 추측해 테스트에 고정하지 않는다.

| 항목 | 확인할 내용 |
| --- | --- |
| 입력과 출력 | 요청 계약과 관찰 가능한 결과 |
| 읽기와 쓰기 | 테이블, 도메인 owner, 조회 조건 |
| transaction | 시작과 종료, rollback이 필요한 실패 |
| 외부 효과 | Redis, S3, HTTP, 이메일, Web Push 호출 순서 |
| 최종 방어선 | DB 제약, 업무 검증, 재시도와 중복 요청 처리 |

기존 코드의 동작과 확정된 문서가 다르면 어느 쪽을 바꿀지 사용자 또는 팀에 확인한다. 특히 권한, 데이터 보관, 외부 부작용의 결과가 바뀌는 선택은 관례만으로 결정하지 않는다.
