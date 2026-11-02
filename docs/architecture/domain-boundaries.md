# 도메인 경계와 책임

Service 코드는 유스케이스 흐름을 직접 드러냅니다.

## 사전 지식

- DDD와 Bounded Context
- Layered Architecture와 계층별 책임
- 관심사 분리, 추상화, 캡슐화, 응집도와 결합도
- Clean Architecture의 의존성 방향(선택)

## 기본 구조

단일 서버로 운영하며 MSA 분리는 계획하지 않습니다. 모듈 분리는 가능성을 열어 두되 기능 개발 속도와 유지보수성을 우선합니다.

프로젝트는 `domain`, `global`, `external` 세 영역으로 구성합니다.

```text
com.fmi
├── domain
│   ├── auth
│   ├── member
│   └── post
│
├── global
│   ├── config
│   ├── error
│   └── web
│
└── external
    ├── slack
    ├── storage
    ├── mail
    ├── messaging
    └── ...
```

- `domain`: 서비스 업무 기능을 관리합니다.
- `global`: 비즈니스 로직 외에 애플리케이션 전체에서 사용하는 코드를 둡니다.
- `external`: 외부 API와 메시지 브로커 등 외부 기술 연동 코드를 둡니다.

## 업무 패키지 구성

상위 패키지는 사용자가 인식하는 큰 업무 영역을 기준으로 구성합니다. 서로 관련된 하위 업무는 같은 상위 패키지에 둡니다.

- 기존 업무를 보조하는 기능은 해당 업무에 포함합니다. Entity, API 또는 Service가 따로 있다는 이유만으로 하위 업무 패키지를 만들지 않습니다.
- 별도의 권한, 생명주기, 정책과 처리 흐름을 갖는 기능은 하위 업무 패키지로 분리할 수 있습니다. 코드 규모보다 업무 규칙의 독립성을 기준으로 판단합니다.
- 분리한 하위 업무는 자신의 `data`, `service`, `repository`, `web` 등을 소유합니다. 실제로 필요한 패키지만 만듭니다.
- 같은 상위 패키지에 속하더라도 다른 하위 업무의 내부 구현을 자유롭게 변경하지 않습니다. 상태 변경은 소유 업무가 제공하는 기능을 통해 수행합니다.

다음은 게시글 영역의 배치 예시이며 현재 코드의 패키지 구조를 나타내지 않습니다. `post/post`, `post/postcomment`라는 이름은 업무 경계와 별도로 팀에서 선택하는 명명 방식입니다.

```text
domain/
└── post/                  # 게시글 업무 영역
    ├── post/              # 게시글과 즐겨찾기
    │   ├── data/
    │   ├── service/
    │   ├── repository/
    │   └── web/
    └── postcomment/       # 게시글 댓글
        ├── data/
        ├── service/
        ├── repository/
        └── web/
```

게시글 즐겨찾기는 게시글에 대한 사용자 선택을 관리하는 보조 기능으로 게시글 업무에 포함합니다. 댓글은 작성과 삭제, 작성자 권한 등 자체 규칙과 처리 흐름을 갖는 하위 업무로 구분합니다. 하위 업무의 분리는 별도 서버, DB 또는 완전히 독립된 도메인을 뜻하지 않습니다.

## 업무 내부 구성

| 영역 | 책임 |
| --- | --- |
| `web` | HTTP 요청과 응답 |
| `scheduler` | 정기 실행 설정과 Service 호출 |
| `listener` | 이벤트 수신, 입력 변환과 Service 호출 |
| `event` | 발행하는 이벤트의 사실과 값 전달 |
| `service` | 유스케이스 흐름과 트랜잭션 |
| `data` | Entity, VO, enum과 순수 Policy |
| `repository` | 데이터 접근과 결과 매핑 |
| `exception` | 해당 업무의 오류 코드 |
| `converter` | 검증된 입력의 Entity 생성 매핑과 형식 변환 |
| `service/internal` | 조회, 검증, 외부 연동 등 분리할 필요가 있는 세부 작업 |

### 업무 모델의 구성

각 업무의 `data`에는 Entity, VO, enum과 순수 Policy를 함께 둡니다. 타입 종류나 파일 개수만으로 `entity`, `vo`, `enums`, `policy` 하위 패키지를 만들지 않습니다. 객체들이 별도의 업무 규칙과 생명주기를 형성하면 하위 업무 패키지 분리를 먼저 검토합니다.

여러 업무에서 사용하더라도 의미를 소유한 업무에 둡니다. 사용처가 많다는 이유로 `global`로 옮기지 않습니다.

### Enum 배치

도메인 enum은 해당 업무의 `data`에 둡니다. 여러 업무가 동일한 의미로 공유하며 특정 업무에 소유권이 없는 공통 enum은 `global/enums`에 둡니다. 도메인 소유 enum을 다른 업무가 참조한다는 이유로 공통화하지 않습니다.

### 생성 검증과 객체 생성

Service는 필요한 데이터를 조회하고, Validator로 생성 조건을 검증한 뒤 Converter로 Entity를 생성하고 저장합니다.

```text
Service
  → 필요한 데이터 조회
  → Validator로 업무 조건 검증
  → Converter로 Entity 생성
  → Repository로 저장
```

| 구성 요소 | 생성 시 책임 |
| --- | --- |
| Controller의 Bean Validation | 필수 입력과 일반적인 형식 검증 |
| Service | 데이터 조회, 실행 순서와 트랜잭션 관리 |
| Validator | 권한, 중복, 상태 등 생성에 필요한 업무 조건 검증 |
| Converter | 검증된 입력을 Entity로 매핑하고 초기 상태 설정 |
| Entity 생성자와 Builder | 전달받은 값으로 객체 생성 |

- Bean Validation을 통과한 입력과 non-null 내부 인자는 다시 검사하지 않습니다.
- Validator에는 입력 형식 검사를 반복하지 않고 업무 판단에 필요한 조건을 작성합니다.
- 일반 게시와 임시저장처럼 허용 조건이 다르면 검증 메서드를 구분합니다.
- Converter는 DB 조회, 업무 검증과 외부 호출을 수행하지 않습니다.
- Entity 생성자와 Builder에는 Validator의 생성 검증을 중복하지 않습니다.
- 새로운 생성 경로에서도 해당 Validator를 거치도록 합니다.
- 입력 정규화가 필요하면 검증 전에 수행합니다. Converter는 검증된 값의 의미를 바꾸지 않습니다.

이 결정은 Entity의 생성 검증과 생성 경로에 한정합니다. 상태 변경 검증과 VO 자체 검증의 위치는 별도 논의 대상으로 남깁니다. 기존 구현의 검증 위치를 이 결정에 따라 일괄 변경하지 않습니다.

### Entity와 VO의 책임

Entity의 생성은 [생성 검증과 객체 생성](#생성-검증과-객체-생성)을 따릅니다. 생성 후 변경에는 `publish()`, `cancel()`, `changeNickname()`처럼 업무 의미가 드러나는 메서드를 사용합니다. 상태 변경 검증을 어느 객체에 둘지는 이 생성 정책으로 확정하지 않습니다.

VO는 불변으로 구성하며 값의 변경이 필요하면 새로운 VO를 생성합니다. 모든 값을 VO로 감싸지 않고 독립적인 업무 의미와 유효성 규칙이 있는 경우에 사용합니다. VO 자체 검증의 위치는 별도 논의합니다.

### Service와 Policy의 책임

| 구성 요소 | 책임 |
| --- | --- |
| Entity | 검증된 값으로 생성하고 생성 후 업무 메서드로 상태 변경 |
| VO | 독립적인 업무 의미를 표현하는 불변 값 |
| 순수 Policy | 여러 객체와 값을 함께 보는 업무 판단, 반복되는 규칙과 계산 |
| Service | 유스케이스 실행 순서, 권한 확인, 트랜잭션과 외부 효과 조정 |
| `service/internal` 협력 객체 | 조회, 검증, 외부 연동 등 분리할 필요가 있는 세부 작업 |
| Repository와 DB | 데이터 접근, 제약조건과 잠금 등을 통한 정합성 보장 |

Policy는 판단에 필요한 객체와 값을 입력받습니다. Repository나 외부 클라이언트를 주입하지 않습니다. 외부 정보가 필요하면 Service 또는 협력 객체가 조회해 전달합니다.

같은 검증을 Entity, Policy와 Validator에 반복하지 않습니다. Entity 생성 조건은 Validator에서 확인합니다. 상태 변경 검증과 VO 자체 검증의 위치는 별도 논의합니다.

### Entity의 의존성과 부수 효과

Entity와 VO는 다음 작업을 수행하지 않습니다.

- Repository를 통한 조회와 저장
- 외부 API, Redis, S3, 메일과 메시징 호출
- 현재 사용자나 요청 컨텍스트 조회
- 트랜잭션과 잠금 제어

판단과 기록에 필요한 현재 시각은 호출자가 전달합니다. Entity 내부에서 직접 현재 시각을 구하지 않습니다.

애플리케이션의 검증만으로 동시 요청의 정합성이 보장되지는 않습니다. 중복과 상태 경합은 업무 요구에 맞는 DB 제약조건, 조건부 갱신과 잠금으로 보호합니다. 구체적인 기준은 [Persistence](persistence.md)를 따릅니다.

### Repository

Repository의 업무별 구성, 기술별 구현 구조와 예외 책임은 [Repository 분리 기준](repository-boundaries.md)을 따릅니다. 조회 기술과 결과 타입은 [Persistence](persistence.md)를 따릅니다.

### Web 계층

Controller의 기능별 구성, 분리 시점과 Service 연결은 [Controller 분리 기준](controller-boundaries.md)을 따릅니다.

요청 DTO는 `web/dto/request`, 응답 DTO는 `web/dto/response`에 둡니다.

Controller는 Bean Validation을 통과한 요청 DTO를 Service에 직접 전달합니다. 계층 구분만을 위해 동일한 필드의 Command나 입력 DTO를 추가하지 않습니다.

파일 이름은 역할에 따라 `[기능명]Request.java` 또는 `[기능명]Response.java`로 작성합니다. DTO는 `class`와 `record` 중 하나를 자유롭게 사용할 수 있습니다.

Swagger 관련 코드는 `web/swagger`에 두며, 파일 이름은 `[기능명]Swagger.java`로 작성합니다.

#### 요청값 검증

Web은 필수 입력과 형식 등 요청 계약을 Bean Validation으로 검증합니다. Entity 생성의 업무 조건은 [생성 검증 기준](#생성-검증과-객체-생성)에 따라 Validator에서 확인합니다.

동일한 업무 규칙을 여러 곳에 독립적으로 정의하지 않습니다. 생성 검증에서 입력 형식과 non-null 내부 인자를 다시 확인하지 않으며, 상태 변경 검증과 VO 자체 검증의 위치는 별도 논의합니다.

좌표의 위도 `-90`에서 `90`, 경도 `-180`에서 `180` 사이는 요청 형식으로 검증합니다. 음수 좌표도 유효합니다. 특정 서비스 지역만 허용하는 조건은 업무 상황에 대한 판단으로 구분합니다.

`@NotBlank`는 공백으로만 구성된 문자열을 거부합니다. 앞뒤 공백 제거는 검증이 아닌 입력 정규화이므로 도메인 객체를 생성하기 전에 `trim()`으로 처리합니다.

조회 쿼리에도 같은 업무 조건이 필요하면 [영속성 기준](persistence.md#조회-조건과-도메인-규칙)에 따라 표현하고 동일한 경계값으로 결과의 일치를 검증합니다.

### 서비스 계층

Service는 업무 정책, 처리 순서, 상태 변경과 트랜잭션 경계를 표현합니다.

기능별 구성, 조회와 변경의 분리 시점, 명명 기준은 [서비스 분리 기준](service-boundaries.md)을 따릅니다.

#### 생성과 상태 변경

Entity 생성은 [Validator 검증 후 Converter로 생성하는 순서](#생성-검증과-객체-생성)를 따릅니다. Entity는 소유 업무의 Converter에서 생성하며 `web`, `repository`, 다른 업무에서 직접 생성하지 않습니다.

생성 후 변경은 소유 업무가 제공하는 업무 메서드로 수행합니다. 이 문서는 상태 변경 검증의 위치를 새로 확정하지 않습니다.

#### 서비스 결과 타입

- 서비스 실행 결과를 전달하는 타입은 해당 업무의 `service/result`에 별도 파일로 둡니다. `service/dto`를 만들거나 `service` 바로 아래에 결과 DTO를 두지 않습니다.
- 타입과 파일 이름은 `[기능명]Result`, `[기능명]Result.java`로 작성하고 Java `record`를 사용합니다.
- 서비스와 `service/internal` 클래스 안에 결과 타입을 중첩 선언하지 않습니다.
- 서비스 결과 타입은 다른 서비스와 Controller에서 사용할 수 있습니다. HTTP 응답 DTO는 `web`에서 서비스 결과를 변환해 만듭니다.
- 독립적인 업무 의미와 불변식이 있는 값은 결과 전달 타입과 구분하여 해당 업무의 `data`에 VO로 둡니다.
- Repository의 조회 결과와 저장소 전용 처리 타입은 서비스 결과라는 이유로 `service/result`에 모으지 않습니다.

JSON 요청과 응답 구조를 표현하는 `web` 계층의 DTO는 중첩 `record`를 사용할 수 있습니다.

### Scheduler, Listener와 Event

Scheduler와 Listener는 Controller처럼 얇은 진입점으로 둡니다. [업무 패키지 구성](#업무-패키지-구성)에 따라 해당 업무 또는 하위 업무 안에 필요한 패키지만 생성합니다.

```text
domain/<업무>/
├── web/
├── scheduler/
├── listener/
├── event/
├── service/
├── data/
└── repository/
```

#### 스케줄러

- Scheduler는 실행 주기와 시간대를 명시하고 Service를 호출합니다. 대상 조회, 업무 검증, 상태 변경, Repository 접근과 외부 호출 조정은 Service에 둡니다.
- 삭제 기준일 같은 업무 판단은 Service가 주입받은 `Clock`을 기준으로 계산합니다.
- 처리량에 따라 전체 또는 묶음별 트랜잭션을 선택하고 Service에 경계를 둡니다.
- 중복 실행, 개별 실패와 전체 실패, 재실행 시 결과를 작업별로 정의합니다. 분산 잠금과 자동 재시도를 모든 작업에 일괄 적용하지 않습니다.

다음은 책임 배치의 가상 예시입니다.

```text
PostCleanupScheduler
  → PostCleanupService
      → 대상 조회
      → 관련 데이터와 이미지 정리
```

#### 이벤트

- 이벤트는 발행 업무의 `event`에 두고 `PostPublishedEvent`처럼 발생한 사실로 이름을 짓습니다.
- Entity나 HTTP DTO 대신 발생한 사실과 수신 처리에 필요한 식별자와 값만 담습니다.
- Listener는 수신 업무의 `listener`에 두며 이벤트 수신, 필요한 입력 변환과 수신 Service 호출을 담당합니다.
- 반드시 함께 성공해야 하는 DB 변경은 Service 직접 호출과 같은 트랜잭션으로 처리합니다.
- 커밋 성공 후 수행할 후속 작업은 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`을 검토합니다.
- 커밋 후 실행과 비동기 실행은 별개입니다. 모든 Listener에 `@Async`를 붙이지 않습니다.
- 커밋 후 처리에서 DB를 변경한다면 수신 Service에 별도의 트랜잭션 경계를 둡니다.
- 프로세스 내부 이벤트는 전달을 영구 보장하지 않습니다. 유실을 허용할 수 없는 작업은 재처리 방법과 저장 방식을 별도로 결정합니다.

### 객체 생성과 변환

Entity 생성 매핑은 Converter가 담당합니다. Builder를 이용한 생성도 Converter에서 수행하며 [Lombok과 Builder 규칙](../contributing/code-style.md#lombok과-builder)을 따릅니다. 생성 순서는 [생성 검증과 객체 생성](#생성-검증과-객체-생성)을 따릅니다.

## 공통 영역

`global`에는 여러 도메인에서 공통으로 사용하는 코드를 둡니다.

- API 응답 형식
- 예외 처리
- 공통 값 객체

공통 Enum의 배치와 도메인 소유권 판단은 [Enum 배치](#enum-배치)를 따릅니다.

`global`에 업무 규칙이나 유스케이스 흐름을 두지 않습니다.

## 외부 연동 영역

`external`에는 외부 API, 메시지 브로커, 스토리지, 메일 등 외부 기술과 연동하는 코드를 둡니다.

## 의존성 방향

```text
web / scheduler / listener → service → internal (선택) → repository
                                ↓
                            entity / vo
```

패키지와 모듈 사이에 순환 의존성을 만들지 않습니다.

## 적용 범위

새 코드와 합의한 리팩터링 대상에 적용합니다. 기존 Entity와 패키지를 일괄 변경하지 않습니다. 실제 적용 대상과 순서는 [Refactoring](../contributing/refactoring.md)에 따라 별도로 정합니다.

## 반드시 지킬 규칙

- 도메인 오류와 정책은 해당 도메인이 소유합니다.
- 이 문서에서 필수로 규정한 패키지 위치와 이름 규칙은 모두 지킵니다.
- 기존 코드를 다른 팀원이 이어서 작업할 때는 특별한 변경 사유가 없다면 해당 영역에서 사용하던 구현 스타일을 유지합니다.

## 팀 논의가 필요한 규칙

다음 항목을 결정하거나 변경하기 전에 팀원과 논의합니다.

- 상태 변경 검증과 VO 자체 검증의 위치
- `internal` 도입 시점
- 구체 구현 직접 의존과 Port 인터페이스 중 선택
- `global`, `domain`, `external` 배치
- 새 도메인 분리 시점

## 부록

다음 사례는 별도 기술 문서로 정리합니다.

- 조회 DTO와 Projection
- Soft Delete
- 이벤트와 외부 호출
