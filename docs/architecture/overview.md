# Architecture Overview

이 문서는 FI-BE의 구조를 찾는 시작점입니다. 세부 규칙은 [Domain Boundaries](domain-boundaries.md), [Persistence](persistence.md), [Decision Guide](decision-guide.md)가 각각 소유합니다.

## 실행 흐름

```text
HTTP / WebSocket -> domain/<업무>/web
Schedule         -> domain/<업무>/scheduler
Event            -> domain/<수신 업무>/listener
  -> domain/<업무>/service               use case와 transaction
  -> service/internal                    필요한 경우 조회, 검증, 계산
  -> repository                          JPA 또는 QueryDSL
  -> MySQL

service -> external                       외부 API, 스토리지, 메시징
service -> domain/<발행 업무>/event        발생한 사실 발행
```

실행 흐름의 `domain/<업무>`는 `domain/post/post`처럼 하위 업무를 포함한 말단 업무 경로를 뜻합니다.

새 기능에서는 입력과 출력, 읽는 데이터와 바꾸는 데이터, transaction, 외부 호출과 실패 뒤 상태를 먼저 적습니다. 다음에 코드 위치를 정합니다. 모든 유스케이스에 `internal`을 만들지는 않습니다.

## 저장소 구조

```text
src/main/java/com/fmi/
├── domain/
│   ├── post/                  # 게시글 업무 영역
│   │   ├── post/              # 게시글, 즐겨찾기와 지도 조회
│   │   │   ├── data/          # Entity, VO, 순수 Policy
│   │   │   │   └── enums/    # 도메인 Enum
│   │   │   ├── repository/   # JPA와 QueryDSL
│   │   │   ├── service/      # 유스케이스와 transaction
│   │   │   │   └── internal/ # 필요할 때만 분리
│   │   │   ├── web/          # HTTP 계약
│   │   │   ├── scheduler/    # 정기 실행 진입점
│   │   │   ├── listener/     # 이벤트 수신 진입점
│   │   │   ├── event/        # 발행하는 이벤트
│   │   │   └── exception/    # 도메인 오류
│   │   ├── comment/
│   │   ├── findcompletion/
│   │   └── review/
│   └── ...
├── global/                # 공통 기술 코드
│   └── enums/             # 공통 Enum
└── external/              # 외부 기술 연동
```

업무 영역과 하위 업무의 패키지 구성은 [업무 패키지 구성](domain-boundaries.md#업무-패키지-구성)을 따릅니다. 위 구조는 게시글 업무 영역과 말단 업무 내부 계층을 표시한 예시입니다.

새 코드의 배치 기준입니다. 레거시 처리 기준은 [AGENTS.md](../../AGENTS.md#레거시-코드와-리팩터링), 패키지 책임은 [Domain Boundaries](domain-boundaries.md)를 따릅니다.

Service의 기능별 구성, QueryService 분리 시점과 이름은 [서비스 분리 기준](service-boundaries.md)을 따릅니다.

HTTP API의 기능별 구성과 Controller 분리 시점은 [Controller 분리 기준](controller-boundaries.md)을 따릅니다.

정기 실행과 이벤트 수신의 책임, 트랜잭션과 실패 처리는 [Scheduler, Listener와 Event](domain-boundaries.md#scheduler-listener와-event)를 따릅니다.

## 기술과 데이터

[Stack](stack.md)에 Java 17, Spring Boot 3.5와 외부 시스템을 정리했습니다. MySQL 스키마는 `src/main/resources/db/migration`의 Flyway migration이 기준입니다. Repository 구성과 기술별 구현 구조는 [Repository 분리 기준](repository-boundaries.md)을 따릅니다. 조회 기술과 결과 타입, JPA 관계, DB 제약과 실제 MySQL 검증 기준은 [Persistence](persistence.md)를 따릅니다.

예를 들어 게시글 코드는 [`PostService`](../../src/main/java/com/fmi/domain/post/post/service/PostService.java), [`PostValidator`](../../src/main/java/com/fmi/domain/post/post/service/internal/PostValidator.java), [`PostRepository`](../../src/main/java/com/fmi/domain/post/post/repository/PostRepository.java)에서 업무 흐름과 책임을 확인할 수 있습니다. 구현 사례이며 필수 클래스 구성은 아닙니다.

## 검토할 질문

1. 누가 데이터를 소유하며 어느 계층이 결과를 결정합니까?
2. `service`에서 transaction과 외부 호출은 어떤 순서로 일어납니까?
3. 동시 요청에서도 지켜야 할 조건의 마지막 방어선은 무엇입니까?
4. Repository가 조회 결과만 반환하고 업무 오류는 올바른 곳에서 결정합니까?
5. 정책서와 코드가 다르면 어느 결정이 최신인지 확인했습니까?
