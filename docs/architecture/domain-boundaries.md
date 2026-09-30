# 도메인 경계와 책임

## 목적

도메인 경계와 책임을 정해 코드 위치를 예측할 수 있게 합니다. 팀원은 담당하지 않은 영역도 빠르게 파악하고 업무에 참여할 수 있어야 합니다.

서비스 계층 코드는 유스케이스 흐름을 드러내야 합니다. 개발자 외 구성원도 코드를 보고 업무 흐름을 이해할 수 있어야 합니다.

코드가 업무 문서 역할을 하도록 작성합니다. 별도 문서 없이도 코드에서 비즈니스를 파악할 수 있어야 합니다.

## 알아야 할 개념

다음 개념을 이해해야 합니다.

- 도메인 주도 설계(Domain-Driven Design, DDD)의 도메인 설계 방식과 제한된 컨텍스트(Bounded Context)
- 레이어드 아키텍처(Layered Architecture)의 구조와 레이어별 책임
- 관심사 분리와 의존성 감소를 위한 추상화
- 캡슐화, 응집도, 결합도
- 책임과 역할

다음 개념을 알면 도움이 됩니다.

- 클린 아키텍처(Clean Architecture)의 관심사 분리와 의존성 흐름

## 기본 구조

프로젝트는 단일 서버로 운영합니다. 도메인 단위 마이크로서비스 아키텍처(Microservices Architecture, MSA) 분리는 현재 계획하지 않습니다.

모듈 단위 분리 가능성은 남겨 둡니다. 현재는 모듈러 모놀리스보다 기능 개발 속도와 유지보수성을 우선합니다.

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

## 도메인 구조

각 도메인은 다음 구조를 기본으로 사용합니다.

```text
domain/post/
├── data/                  # Entity, VO, Enum
├── repository/            # DB 접근
├── service/               # 유스케이스
│   └── internal/          # 재사용하는 세부 작업. 필요할 때만 생성
├── web/                   # Controller, Request/Response DTO, Swagger
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── swagger/
└── converter/             # 형식 변환. 필요할 때만 생성
```

### 필수 영역

| 영역 | 책임 |
| --- | --- |
| `web` | HTTP 요청과 응답을 처리합니다. |
| `service` | 유스케이스 흐름과 트랜잭션 경계를 표현합니다. |
| `data` | 상태, 값 객체(Value Object, VO), 정책을 관리합니다. |
| `repository` | 데이터베이스에 접근합니다. |
| `exception` | 도메인 오류 코드를 관리합니다. |

### 선택 영역

| 영역 | 책임 |
| --- | --- |
| `converter` | DTO와 Entity를 변환합니다. |
| `internal` | 조회, 검증, 상태 변경처럼 변경 이유가 다른 세부 작업 하나를 담당합니다. |

### Repository

Repository는 조회 결과를 Entity, Projection, `Optional` 또는 빈 컬렉션으로 반환합니다. 조회 결과가 없다는 이유로 도메인 오류 코드나 비즈니스 예외를 생성하지 않습니다.

조회 결과가 반드시 필요한 유스케이스는 Service 또는 `service/internal`의 Reader에서 결과를 확인하고 비즈니스 예외로 변환합니다. Repository는 데이터베이스 접근 중 발생한 기술 예외만 전달하거나 영속성 계층의 예외로 변환할 수 있습니다.

### Web 계층

요청 DTO는 `web/dto/request`, 응답 DTO는 `web/dto/response`에 둡니다.

파일 이름은 역할에 따라 `[기능명]Request.java` 또는 `[기능명]Response.java`로 작성합니다. DTO는 `class`와 `record` 중 하나를 자유롭게 사용할 수 있습니다.

Swagger 관련 코드는 `web/swagger`에 두며, 파일 이름은 `[기능명]Swagger.java`로 작성합니다.

#### 요청값 검증

Bean Validation은 요청값의 존재 여부와 일반적인 입력 형식을 검증합니다. 필수 값과 공백 문자열은 `@NotNull`, `@NotBlank`, `@NotEmpty`로 검증합니다. 이메일 형식, ID의 양수 여부, 페이지 크기처럼 요청 자체가 성립하기 위한 조건도 Bean Validation으로 검증합니다.

좌표는 위도 `-90`에서 `90`, 경도 `-180`에서 `180` 사이인지 Bean Validation으로 검증합니다. 음수 좌표도 유효하므로 음수라는 이유만으로 거부하지 않습니다. 특정 서비스 지역만 허용하는 조건은 `service/internal`의 Validator에서 검증합니다.

글자 수, 허용 문자, 중복, 권한, 상태처럼 서비스 정책에 따라 달라지는 조건은 `service/internal`의 Validator에서 검증합니다. Bean Validation과 Validator에 같은 조건을 중복해서 작성하지 않습니다.

`@NotBlank`는 공백으로만 구성된 문자열을 거부하는 데 사용합니다. 앞뒤 공백 제거는 검증이 아닌 입력 정규화이므로 도메인 객체를 생성하기 전에 `trim()`으로 처리합니다.

### 서비스 계층

서비스 계층은 유스케이스를 담당합니다. 업무 정책, 처리 순서, 상태 관리, 트랜잭션 범위를 표현합니다.

다른 이해관계자가 읽어도 업무 흐름을 이해할 수 있게 작성합니다. 서비스 메서드는 코드 자체로 업무 문서 역할을 해야 합니다.

```java
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostReader postReader;
    private final PostValidator postValidator;
    private final EventPublisher eventPublisher;

    @Transactional
    public void publish(Long postId, Long authorId) {
        Post post = postReader.read(postId);

        postValidator.validate(post, authorId);
        post.publish();

        eventPublisher.publish(PostPublishedEvent.from(post));
    }
}
```

`PostService.publish()`는 다음 흐름을 나타냅니다.

1. 게시글을 조회합니다.
2. 게시글을 발행할 수 있는지 검증합니다.
3. 게시글 상태를 발행으로 변경합니다.
4. 발행 완료 이벤트를 발행합니다. 알림 도메인이 이벤트를 구독할 수 있습니다.

#### 검증과 상태 변경

비즈니스 검증은 `service/internal`의 `[대상]Validator`에서 수행합니다. Entity나 VO의 생성자와 정적 팩토리에 같은 검증을 중복해서 작성하지 않습니다.

Entity는 `setStatus()`처럼 구현 방식을 드러내는 setter 대신 `publish()`, `cancel()`처럼 의미 있는 상태 변경 메서드를 제공합니다. `internal`은 상태를 변경할 수 있는지 판단하는 세부 도메인 로직을 담당합니다. Service는 검증 후 Entity의 상태 변경 메서드를 호출합니다.

도메인 객체는 해당 도메인의 `service`, `service/internal`, `converter`에서 생성합니다. `web`, `repository`, 다른 도메인에서 도메인 객체를 직접 생성하지 않습니다.

#### 서비스 중첩 타입

서비스와 `service/internal` 클래스 안에 `record`를 중첩해서 선언하지 않습니다. 반환값과 계산 중간값에 모두 적용합니다. 서비스 내부에 타입이 숨겨져 있으면 타입의 의미와 사용 범위를 추적하기 어렵고, 서비스가 담당하는 책임도 불분명해질 수 있습니다.

반환값이나 계산 중간값이 하나의 개념을 나타낸다면 값 객체(Value Object, VO)로 모델링합니다. 해당 도메인의 `data` 패키지에 독립된 파일로 둡니다.

서비스에서만 사용하는 반환 값이 필요하다면 다음 순서로 구조를 검토합니다.

1. 반환하는 값이 서로 다른 작업의 결과라면 서비스 메서드를 분리합니다.
2. 여러 값이 항상 함께 사용되는 하나의 개념이라면 별도의 VO로 정의합니다.
3. 서비스가 여러 책임을 함께 처리하고 있지 않은지 확인합니다.

Java 메서드가 하나의 객체만 반환할 수 있다는 이유만으로 값을 임시로 묶지 않습니다.

JSON 요청과 응답 구조를 표현하는 `web` 계층의 DTO는 중첩 `record`를 사용할 수 있습니다.

### 객체 생성과 변환

유스케이스에서 도메인 객체를 생성하거나 조합하는 방식은 다음 중 자유롭게 선택할 수 있습니다.

- Builder를 사용해 직접 조합합니다.
- Converter를 사용합니다.
- 생성자를 사용합니다.

## 공통 영역

`global`에는 여러 도메인에서 공통으로 사용하는 코드를 둡니다.

- API 응답 형식
- 예외 처리
- 공통 값 객체

`global`에 업무 규칙이나 유스케이스 흐름을 두지 않습니다.

## 외부 연동 영역

`external`에는 외부 API, 메시지 브로커, 스토리지, 메일 등 외부 기술과 연동하는 코드를 둡니다.

## 의존성 방향

```text
web → service → internal (선택) → repository
                    ↓
                entity / vo
```

패키지와 모듈 사이에 순환 의존성을 만들지 않습니다.

## 반드시 지킬 규칙

- 도메인 오류와 정책은 해당 도메인이 소유합니다.
- 이 문서에서 필수로 규정한 패키지 위치와 이름 규칙은 모두 지킵니다.
- 기존 코드를 다른 팀원이 이어서 작업할 때는 특별한 변경 사유가 없다면 해당 영역에서 사용하던 구현 스타일을 유지합니다.

## 팀 논의가 필요한 규칙

다음 항목을 결정하거나 변경하기 전에 팀원과 논의합니다.

- `internal` 도입 시점
- 구체 구현 직접 의존과 Port 인터페이스 중 선택
- `global`, `domain`, `external` 배치
- 새 도메인 분리 시점

## 부록

다음 사례는 별도 기술 문서로 정리합니다.

- 조회 DTO와 Projection
- Soft Delete
- 이벤트와 외부 호출
