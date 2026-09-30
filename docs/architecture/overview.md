# Architecture Overview

이 문서는 FI-BE의 구조를 찾는 시작점이다. 세부 규칙은 [Domain Boundaries](domain-boundaries.md), [Persistence](persistence.md), [Decision Guide](decision-guide.md)가 각각 소유한다.

## 실행 흐름

```text
HTTP / WebSocket / Schedule
  -> domain/<업무>/web 또는 listener
  -> domain/<업무>/service               use case와 transaction
  -> service/internal                    필요한 경우 조회, 검증, 계산
  -> repository                          JPA 또는 QueryDSL
  -> MySQL

service -> external                       외부 API, 스토리지, 메시징
```

새 기능에서는 입력과 출력, 읽는 데이터와 바꾸는 데이터, transaction, 외부 호출과 실패 뒤 상태를 먼저 적는다. 다음에 코드 위치를 정한다. 모든 유스케이스에 `internal`을 만들지는 않는다.

## 저장소 구조

```text
src/main/java/com/fmi/
├── domain/
│   ├── post/
│   │   ├── data/          # Entity, 값 객체와 상태
│   │   ├── repository/    # JPA와 QueryDSL
│   │   ├── service/       # 유스케이스와 transaction
│   │   │   └── internal/  # 필요할 때만 분리
│   │   ├── web/           # HTTP 계약
│   │   └── exception/     # 도메인 오류
│   └── ...
├── global/                # 공통 기술 코드
└── external/              # 외부 기술 연동
```

실제 저장소에는 이전에 만들어진 다른 배치도 있다. 이 트리는 새 코드를 놓을 때 쓰는 기준이며, 이 문서만을 이유로 기존 코드를 일괄 이동하지 않는다. [Domain Boundaries](domain-boundaries.md)에 필수 영역과 선택 영역, 책임과 팀 논의 조건이 있다.

## 기술과 데이터

[Stack](stack.md)에 Java 17, Spring Boot 3.5와 외부 시스템을 정리했다. MySQL 스키마는 `src/main/resources/db/migration`의 Flyway migration이 기준이다. JPA 관계, QueryDSL, DB 제약과 실제 MySQL 검증 기준은 [Persistence](persistence.md)를 따른다.

예를 들어 게시글 코드는 [`PostService`](../../src/main/java/com/fmi/domain/post/service/PostService.java), [`PostValidator`](../../src/main/java/com/fmi/domain/post/service/internal/PostValidator.java), [`PostRepository`](../../src/main/java/com/fmi/domain/post/repository/PostRepository.java)에서 업무 흐름과 책임을 확인할 수 있다. 이 경로는 현재 구현의 사례이며 모든 기능에 같은 클래스 수를 요구하지 않는다.

## 검토할 질문

1. 누가 데이터를 소유하며 어느 계층이 결과를 결정하는가?
2. `service`에서 transaction과 외부 호출은 어떤 순서로 일어나는가?
3. 동시 요청에서도 지켜야 할 조건의 마지막 방어선은 무엇인가?
4. Repository가 조회 결과만 반환하고 업무 오류는 올바른 곳에서 결정하는가?
5. 정책서와 코드가 다르면 어느 결정이 최신인지 확인했는가?
