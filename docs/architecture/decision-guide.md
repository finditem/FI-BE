# Decision Guide

[Architecture Overview](overview.md)를 확인하고 필요한 위치에만 구조를 추가합니다. 미확정 정책은 [AGENTS.md](../../AGENTS.md#문서에-없는-정책)를 따릅니다.

| 고민 | 먼저 볼 기준 | 결정 조건 |
| --- | --- | --- |
| 새 도메인 또는 `global` | [Domain Boundaries](domain-boundaries.md) | 데이터 owner와 변경 이유가 분명합니까? 경계가 애매하면 팀과 논의합니다. |
| `service/internal` 분리 | [Domain Boundaries](domain-boundaries.md) | 조회, 검증, 계산 등의 독립 책임과 재사용 필요가 실제로 있습니까? 분리 시점은 팀과 논의합니다. |
| JPA Repository, QueryDSL, native SQL | [Persistence](persistence.md) | 단순 저장입니까, 목록과 집계입니까? 복잡한 QueryDSL보다 SQL이 의도를 명확히 드러냅니까? 선택 이유를 작업 보고에서 설명하고 MySQL 테스트를 작성합니다. |
| JPA 연관관계 | [Persistence](persistence.md) | ID만 필요합니까, 연관 객체의 의미와 데이터가 필요합니까? 필요한 관계를 매핑하고 조회 시 N+1과 과도한 로딩을 방지합니다. |
| DB 제약 | [Persistence](persistence.md) | 참조 무결성과 유일성을 동시 요청에서도 보장해야 합니까? |
| port와 외부 client | [Domain Boundaries](domain-boundaries.md) | 도입 기준은 팀 논의 대상입니다. 공급자 API를 업무 규칙으로 끌어오지 않습니다. |
| 오류 발생 위치 | [Error Handling](../contributing/error-handling.md) | 입력 형식, 업무 검증, 조회 부재, 인프라 실패 중 어디서 결정할 오류입니까? |

## 변경 범위 기록

다음 표는 구현 전에 채울 수 있습니다. 모르는 업무 조건은 추측해 테스트에 고정하지 않습니다.

| 항목 | 확인할 내용 |
| --- | --- |
| 입력과 출력 | 요청 계약과 관찰 가능한 결과 |
| 읽기와 쓰기 | 테이블, 도메인 owner, 조회 조건 |
| transaction | 시작과 종료, rollback이 필요한 실패 |
| 외부 효과 | Redis, S3, HTTP, 이메일, Web Push 호출 순서 |
| 최종 방어선 | DB 제약, 업무 검증, 재시도와 중복 요청 처리 |

기존 코드의 동작과 확정된 문서가 다르면 어느 쪽을 바꿀지 사용자 또는 팀에 확인합니다. 특히 권한, 데이터 보관, 외부 부작용의 결과가 바뀌는 선택은 관례만으로 결정하지 않습니다.
