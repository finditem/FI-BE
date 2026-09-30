# 테스트 코드 컨벤션

## 목적

테스트 코드를 같은 방식으로 작성해 읽기와 수정 비용을 줄입니다. 테스트는 도메인 정책, 데이터 상태, 동시성 상황에서 기대한 결과를 확인해야 합니다.

## 전제

아래 컨벤션을 지킬 수 없는 상황이 생기거나, 더 좋은 방안이 있다면 논의 후 예외 사항으로 적용하거나, 컨벤션을 갱신할 수 있습니다.

- 테스트는 동작을 설명하는 실행 가능한 문서입니다.
- 저장소와 연동하는 기능은 mock이 아닌 실제 저장소로 검증합니다.

## 테스트 범위

테스트 대상의 책임과 외부 의존성에 따라 테스트 방식을 정합니다. 같은 동작을 단위 테스트와 통합 테스트에서 중복으로 검증하지 않습니다.

| 대상 | 테스트 방식 | 검증 범위 |
| --- | --- | --- |
| 도메인 Entity, Value Object, 도메인 정책, 순수 로직 | 단위 테스트 | 상태 전이, 정책, 계산, 예외 |
| Usecase Service, Repository | Testcontainers 통합 테스트 | 트랜잭션, JPA 연관관계, MySQL, Redis, S3 |
| 외부 HTTP 연동 | WireMock 통합 테스트 | 요청 형식, 응답 변환, 오류 처리 |
| Web | 보안·쿠키 테스트 | 인증·인가 실패, 쿠키 발급·갱신·만료 |

Usecase Service는 Mockito 단위 테스트로 작성하지 않습니다. Spring context에서 실제 bean을 조립하고 Testcontainers와 WireMock으로 유스케이스 전체 흐름을 검증합니다.

Web 테스트는 보안과 쿠키 계약만 검증합니다. DTO 바인딩, Bean Validation, 성공·실패 응답 body 형식은 검증하지 않습니다.

## 테스트 설정

통합 테스트 설정은 `src/test/resources/application-test.yml`에서 관리합니다. 통합 테스트는 `test` 프로필로 실행합니다.

애플리케이션 설정 항목을 추가하거나 삭제할 때는 `application-local.yml`과 `application-test.yml`을 함께 확인합니다. 포트, 로깅, 데이터베이스 초기화 방식처럼 환경마다 다른 동작이 필요한 값은 테스트 환경에 맞게 설정합니다.

인증 정보와 외부 서비스 설정에는 실제 값을 사용하지 않습니다. `test-` 접두사를 붙인 임의 값처럼 운영 환경에서 사용할 수 없는 테스트 전용 값을 입력합니다. 외부 HTTP 연동 주소는 WireMock 서버를 가리키도록 설정합니다.

## 테스트 패키지

테스트는 테스트 대상과 같은 패키지에 둡니다. 운영 코드와 테스트 코드의 경로를 같게 유지해 대상 코드를 바로 찾을 수 있어야 합니다.

```text
src/main/java/com/fmi/
└── domain/auth/service/
    └── AuthService.java

src/test/java/com/fmi/
└── domain/auth/service/
    └── AuthServiceTest.java
```

테스트 클래스 이름은 `<대상 클래스>Test`로 작성합니다. 하나의 클래스에서 여러 동작을 검증할 때는 `@Nested`로 구분합니다. 동작마다 테스트 클래스를 나누지 않습니다.

## fixture와 mock

Instancio는 도메인 Entity와 DTO의 기본 fixture를 생성합니다. 테스트 결과에 영향을 주는 예외 조건, 경계값, 권한, 상태값은 테스트 코드에서 직접 지정합니다.

```java
User user = Instancio.of(User.class)
        .set(field(User::getEmail), "member@finditem.kr")
        .set(field(User::getPreferredLanguage), LanguageCode.EN)
        .create();
```

fixture는 각 테스트 본문에서 생성합니다. 공통 Fixture 클래스, Object Mother, `private` helper를 만들지 않습니다. 테스트에서 사용하는 데이터와 조건을 한눈에 파악할 수 있어야 합니다.

Mockito는 단위 테스트의 JVM 내부 협력 객체만 대체합니다. Usecase Service와 Repository 통합 테스트의 Repository, Redis, S3는 Mockito로 대체하지 않습니다. 외부 HTTP 연동은 WireMock으로 대체합니다.

Mockito 단위 테스트는 `MockitoExtension`을 사용합니다. SUT(System Under Test)는 `@InjectMocks`로 선언하고, 협력 객체는 테스트 클래스의 `@Mock` 필드로 선언합니다. 생성자 호출과 `@InjectMocks`를 섞어 SUT를 만들지 않습니다.

```java
@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordValidator passwordValidator;
}
```

## 테스트 구조

테스트 클래스는 대상 클래스를 나타냅니다. `@Nested`와 `@DisplayName`은 Describe–Context–It(DCI) 순서로 동작, 조건, 기대 결과를 표현합니다.

| 단계 | Java 이름 | `@DisplayName` |
| --- | --- | --- |
| 테스트 클래스 | `<SUT>Test` | `<SUT>` |
| Describe | `Describe<동작>` | `<동작>할 때` |
| Context | `Context<조건>` | `<조건>이면` 또는 `<조건>일 때` |
| It | `it<기대결과>` | `<기대 결과>한다` |

Java 식별자는 `PascalCase`와 `camelCase`를 사용합니다. `@DisplayName`은 한국어로 작성하고 대상, 조건, 결과를 생략하지 않습니다.

조건에 따라 결과가 다르면 Context를 나눕니다. 조건이 없는 단일 시나리오는 Describe 아래에 It을 바로 작성합니다.

```java
@DisplayName("AuthService")
class AuthServiceTest {

    @Nested
    @DisplayName("회원 가입할 때")
    class DescribeSignup {

        @Nested
        @DisplayName("이메일 인증 정보가 있으면")
        class ContextWithEmailVerification {

            @Test
            @DisplayName("인증 정보를 소비한 뒤 사용자를 저장한다")
            void itSavesVerifiedUserAfterConsumingVerification() {
                // given
                // when
                // then
            }
        }
    }
}
```

`성공`, `실패`, `정상 동작`, `테스트`처럼 결과를 알 수 없는 이름은 사용하지 않습니다. 예외는 예외 이름 또는 오류 코드를, 외부 효과는 호출 대상과 결과를 이름에 적습니다.

## 검증 기준

- 테스트 하나는 하나의 핵심 시나리오를 검증합니다.
- 결과값 assertion은 시나리오의 핵심 결과를 검증합니다.
- mock 검증은 외부 효과, 호출 금지, 호출 순서처럼 동작 계약에 의미가 있을 때만 작성합니다.
- 구현 세부 사항을 복제해 검증하지 않습니다.
- 시간, 난수, 현재 사용자처럼 변하는 입력은 고정하거나 주입합니다.

## 테스트를 고르는 세 가지 질문

테스트를 작성하기 전에 **무엇을 검증하는지**, **어디까지 함께 실행해야 결과를 관찰할 수 있는지**, **어떤 위험을 막는지**를 각각 적습니다. 정책이라는 이유만으로 무조건 단위 테스트를 선택하거나, 유스케이스라는 이유만으로 모든 시나리오를 같은 통합 테스트에 넣지 않습니다. 이 저장소의 [테스트 범위](#테스트-범위)를 지키면서 위험을 실제로 관찰할 수 있는 가장 작은 범위를 고릅니다.

테스트의 목적도 분명히 합니다. 구현 확인과 회귀 방지, 허용과 거절 조건의 명세, 책임이 불분명한 설계의 발견, 독자를 위한 업무 설명 중 무엇을 맡는지 확인합니다. 하나의 테스트가 여러 목적을 수행할 수 있습니다.

| 검증할 위험 | 우선 선택 | 관찰할 결과 |
| --- | --- | --- |
| 순수한 정책, 계산, 상태 전이 | 도메인 단위 테스트 | 허용, 거절, 경계값과 상태 |
| 유스케이스의 주요 흐름 | Spring context와 필요한 Testcontainers를 포함한 통합 테스트 | 실제 저장 결과와 외부 효과 |
| transaction commit과 rollback | 실제 MySQL transaction 통합 테스트 | 실패 후 남은 DB 상태 |
| JPA 매핑, QueryDSL 조건, DB 제약 | 실제 MySQL Testcontainers 테스트 | 실행 결과, 매핑과 정합성 |
| 동시성, 잠금, 격리 | 병렬 요청을 포함한 실제 DB 테스트 | 경합 후 최종 상태 |
| 외부 HTTP 응답과 오류 변환 | WireMock 통합 테스트 | 요청, 응답과 실패 처리 |
| Web 보안과 쿠키 | Web 보안 테스트 | 인증, 인가와 쿠키 계약 |

Mock 호출 횟수만으로 DB commit, rollback, JPA flush, QueryDSL 결과나 외부 시스템의 실제 계약을 증명했다고 쓰지 않습니다. 반대로 이미 순수 정책 테스트가 설명한 모든 입력 조합을 통합 테스트에서 반복하지 않습니다.

## 정책 테스트는 결정 경계를 보여줍니다

정책 테스트는 대표적인 성공 하나와 임의의 실패 하나에서 끝나지 않습니다. 허용되는 최소값, 바로 바깥 값, 상태 조합과 거절 이유를 구분해 정책의 경계를 읽을 수 있게 합니다. 예를 들어 길이 제한이 있다면 정확한 최소 길이와 그보다 1 짧은 길이를 각각 검증합니다. 실제 기준값은 [업무 정책](../policies/overview.md)과 구현에서 확인합니다.

```java
@DisplayName("PasswordPolicy")
class PasswordPolicyTest {

    @Nested
    @DisplayName("비밀번호를 검증할 때")
    class DescribeValidate {

        @Test
        @DisplayName("허용되는 최소 길이면 통과한다")
        void itAcceptsMinimumLength() {
            // given: 정책에서 확인한 최소 길이의 비밀번호
            // when: 정책을 검증한다
            // then: 예외 없이 통과한다
        }

        @Nested
        @DisplayName("최소 길이보다 짧으면")
        class ContextBelowMinimumLength {

            @Test
            @DisplayName("약한 비밀번호 오류를 반환한다")
            void itRejectsBelowMinimumLength() {
                // given: 최소 길이보다 1 짧은 비밀번호
                // when: 정책을 검증한다
                // then: 정해진 오류를 확인한다
            }
        }
    }
}
```

이 코드는 테스트 구성 예시이며 현재 정책의 최소 길이를 새로 정하지 않습니다. 현재 사례는 [`PasswordPolicyTest`](../../src/test/java/com/fmi/domain/user/data/PasswordPolicyTest.java)와 [`PostValidatorTest`](../../src/test/java/com/fmi/domain/post/service/internal/PostValidatorTest.java)에서 확인합니다. 정책과 QueryDSL 조건이 같은 규칙을 표현한다면 동일한 경계값과 상태 조합을 단위 테스트와 MySQL 통합 테스트에서 각각 관찰합니다. 조회 조건의 작성 기준은 [Persistence](../architecture/persistence.md)를 따릅니다.

## 유스케이스 테스트는 절차와 결과를 보여줍니다

유스케이스 테스트는 독자가 `given → when → then`을 순서대로 읽으며 준비된 상태, 실행한 한 가지 행동, 그 뒤에 남은 데이터와 외부 효과를 이해할 수 있어야 합니다. 중요한 입력, 저장 결과와 호출 순서를 fixture나 helper에 감추지 않습니다. `when`에는 검증 대상의 행동을 한 번 실행하고, `then`에서 관찰 가능한 결과를 확인합니다. 여러 assertion이 하나의 업무 결과를 설명하면 함께 둘 수 있지만 독립된 정책과 실패 이유는 테스트를 나눕니다.

다음 시나리오를 한 테스트 메서드에 모두 넣지 않고 각각 DCI의 `Context`와 `It`로 표현합니다.

| 순서 | 시나리오 | 직접 확인할 결과 |
| --- | --- | --- |
| 1 | 유효한 요청 | 저장된 Entity와 반환 결과 |
| 2 | 업무 조건 위반 | 오류와 저장되지 않은 상태 |
| 3 | 저장 실패 | 뒤따르는 이벤트 또는 알림의 미발행 |
| 4 | 외부 연동 실패 | transaction 결과와 이미 발생한 외부 효과 |
| 5 | 중복 또는 동시 요청 | DB 제약과 최종 데이터 상태 |

```java
@Nested
@DisplayName("회원 가입할 때")
class DescribeSignup {

    @Nested
    @DisplayName("인증된 이메일과 유효한 가입 정보가 있으면")
    class ContextWithVerifiedEmail {

        @Test
        @DisplayName("사용자를 저장하고 가입 완료 결과를 반환한다")
        void itPersistsUserAndReturnsSignupResult() {
            // given: 이메일 인증 상태와 명시적인 가입 정보
            // when: 회원 가입을 한 번 실행
            // then: 실제 DB에서 사용자 상태와 결과를 조회
        }
    }
}
```

위 코드는 흐름을 보여주는 골격입니다. 실제 [`PlaceServiceTest`](../../src/test/java/com/fmi/domain/place/service/PlaceServiceTest.java)는 장소 생성의 선행 조건을 만들고, `placeService.create()`를 실행한 뒤 Repository에서 장소와 요일별 영업시간을 다시 조회합니다. 테스트 본문에 요청의 주요 값과 최종 DB 상태가 보입니다.

```java
// given
PlaceUpsertCommand command = new PlaceUpsertCommand(
        "성수 카페", "서울 성동구", 37.54, 127.05, "성수역", 320, PlaceType.CAFE,
        null, null, schedules);

// when
Long placeId = placeService.create(
        command, new MockMultipartFile("thumbnail", "thumbnail.png", "image/png", new byte[] {1}));

// then
Place place = placeRepository.findById(placeId).orElseThrow();
assertThat(place.getThumbnailUrl()).isEqualTo("https://storage.test/thumbnail.png");
assertThat(placeBusinessHourRepository.findAllByPlaceId(placeId)).hasSize(7);
```

이 발췌에는 `schedules`를 준비하는 부분이 생략되어 있으므로 전체 테스트는 연결한 실제 파일에서 확인합니다. 이 테스트는 MySQL 저장 결과를 관찰하지만 테스트 설정에서 S3는 대체하므로 실제 S3 프로토콜까지 검증하지는 않습니다.

현재 [`AuthServiceTest`](../../src/test/java/com/fmi/domain/auth/service/AuthServiceTest.java)는 DCI 구조와 일부 호출 순서를 보여주지만 Mockito로 Repository를 대체합니다. 따라서 그 테스트만으로 실제 DB 저장, 제약 조건 또는 rollback이 검증됐다고 주장하지 않습니다. 새 유스케이스 통합 테스트는 [테스트 범위](#테스트-범위)에 맞는 실제 저장소에서 최종 상태를 확인합니다.

## 이름과 Given, When, Then

DCI의 `Describe`는 행동, `Context`는 결과를 바꾸는 조건, `It`은 관찰 가능한 결과를 표현합니다. 조건이 없는 단일 시나리오는 기존 기준대로 `Context`를 생략할 수 있습니다. 이름에 `성공`, `정상 동작` 대신 저장, 거절, 반환, 발행 금지 같은 업무 결과를 적습니다. 새로운 정책이나 중요한 유스케이스의 이름은 사람이 조건과 결과를 검토합니다. 이미 확정된 이름을 같은 뜻으로 반복 적용할 때마다 다시 승인을 요구하지 않습니다.

테스트 본문에는 `// given`, `// when`, `// then`을 두고 각 구간의 중요한 값을 직접 드러냅니다. 예외를 검증할 때도 `when`에서 실행 결과를 포착하고 `then`에서 오류와 부수 효과를 확인할 수 있게 작성합니다. `Instancio`는 결과와 무관한 유효한 값에만 사용하고 경계값, 상태, 시간과 식별자는 본문에 명시합니다. fixture와 mock의 세부 제한은 위 [fixture와 mock](#fixture와-mock) 기준을 따릅니다.

## 작성 전 확인

- 검증 대상과 가장 위험한 실패를 한 문장으로 설명할 수 있는가?
- 선택한 테스트에서 실제로 그 결과를 관찰할 수 있는가?
- 정책의 허용과 거절 경계가 드러나는가?
- 유스케이스의 DB 변경, transaction, 외부 효과와 실패 뒤 상태가 드러나는가?
- DCI 이름과 `DisplayName`이 구현 방식 대신 업무 결과를 설명하는가?
- Given, When, Then에서 핵심 입력과 결과를 바로 찾을 수 있는가?
- mock 검증을 실제 저장소나 외부 계약의 증거로 잘못 해석하지 않았는가?

## 팀 논의가 필요한 규칙

다음 항목을 결정하거나 변경하기 전에 팀원과 논의합니다.

- Instancio가 JPA 연관관계까지 자동 생성할 때 허용할 깊이와 제외 기준
