# 코딩 컨벤션

## 코드 검사

### Spotless

개발자는 코드 변경 후 아래 명령어로 형식을 적용합니다.

```bash
./gradlew spotlessApply
```

지속적 통합(Continuous Integration, CI)은 아래 명령어로 형식을 검증합니다. 검증에 실패하면 병합할 수 없습니다.

```bash
./gradlew spotlessCheck
```

Spotless는 다음 규칙을 적용합니다.

- `palantirJavaFormat()`: Palantir Code Style Guide에 따라 들여쓰기, 줄바꿈, 공백을 맞춥니다.
- `formatAnnotations()`: 어노테이션 줄바꿈과 배치를 맞춥니다.
- `removeUnusedImports()`: 사용하지 않는 `import` 문을 제거합니다.
- `trimTrailingWhitespace()`: 줄 끝 공백을 제거합니다.
- `endWithNewline()`: 파일 끝에 줄바꿈을 추가합니다.

### import

Java 코드 본문에서 FQCN(Fully Qualified Class Name)을 사용하지 않습니다. 클래스와 정적 멤버는 `import` 또는 `import static`으로 선언하고 단순 이름으로 참조합니다.

같은 단순 이름을 가진 타입이 충돌하면 FQCN으로 구분하지 않습니다. 각 타입의 책임이 드러나도록 이름을 변경합니다.

### Commitlint

Commitlint는 Git 커밋 메시지가 커밋 컨벤션을 따르는지 검사합니다. Git hook이 커밋할 때마다 Commitlint를 실행합니다.

### 도입 후보

다음 도구는 프로젝트가 안정화된 뒤 도입 여부를 논의합니다.

- SpotBugs
- ArchUnit
- SonarQube
- SonarLint

## 이름 규칙

클래스 이름은 `[도메인 대상] + [수행 책임]`으로 작성합니다. 이름만 보고 클래스 책임을 파악할 수 있어야 합니다.

예를 들어 주문 번호를 생성하는 클래스는 `OrderNumberGenerator`로 작성합니다.

### 서비스 클래스 이름

서비스 클래스 이름은 담당하는 유스케이스를 드러내야 합니다.

`QueryService`와 `CommandService`처럼 조회와 변경이라는 기술적 분류만 나타내는 이름은 피합니다. 클래스가 처리하는 업무를 기준으로 `[도메인 대상] + [유스케이스 역할] + Service` 형식의 이름을 사용합니다.

```text
// 비권장
PostQueryService
UserCommandService

// 권장
PostSearchService
PostDetailService
UserRegistrationService
UserProfileChangeService
```

하나의 `QueryService`나 `CommandService`가 서로 다른 유스케이스를 함께 담당한다면 클래스 분리가 필요한지도 검토합니다.

조회와 명령의 분리 자체가 아키텍처상의 책임인 경우에는 팀 논의를 거쳐 예외로 사용할 수 있습니다.

### internal 클래스 이름

`internal` 클래스 이름은 데이터 조회와 저장 순서, 사용 기술보다 유스케이스에서 수행하는 업무 책임을 나타냅니다. 도메인 대상과 업무 결과를 먼저 정하고 실제 책임에 맞는 접미사를 사용합니다.

```text
// 비권장
UserRepositoryChecker
SignupDataProcessor
PlaceListManager

// 권장
SignupEligibilityValidator
PlaceOperationStatusCalculator
PopupClosingDateTimeCalculator
```

`Manager`, `Processor`, `Handler`처럼 책임 범위가 넓은 이름만으로 역할을 표현하지 않습니다. 이름만으로 책임을 정하기 어렵다면 Repository나 사용 기술의 이름을 붙이지 않습니다. 해당 유스케이스에서 무엇을 결정하거나 검증하는지 팀원과 논의합니다.

### 클래스 접미사

| 접미사 | 사용하는 경우 | 예시 | 사용하지 않는 경우 |
| --- | --- | --- | --- |
| `Generator` | 새 값, 번호, 식별자, 코드, 문서를 생성할 때 | `OrderNumberGenerator`, `InviteCodeGenerator` | 단순 조회, 선택, 조립 |
| `Factory` | 객체를 생성하거나 조립해 반환할 때 | `PaymentCommandFactory`, `OrderLineFactory` | 도메인 상태를 변경할 때 |
| `Builder` | 복잡한 객체를 단계적으로 조립할 때 | `OrderSummaryBuilder`, `SettlementReportBuilder` | 단순 생성 로직 |
| `Resolver` | 후보, 문맥, 규칙으로 하나를 선택할 때 | `PaymentMethodResolver`, `ShippingPolicyResolver` | 항상 같은 값을 계산하거나 생성할 때 |
| `Validator` | 규칙 충족 여부를 검증하고 위반을 알릴 때 | `CouponEligibilityValidator`, `OrderCancellationValidator` | 검증 뒤 상태까지 변경할 때 |
| `Checker` | `boolean`을 반환하는 가벼운 조건을 확인할 때 | `DuplicateEmailChecker`, `OrderOwnershipChecker` | 복잡한 오류를 수집하거나 정책을 검증할 때 |
| `Handler` | 명령, 이벤트, 상태 전이, 요청 하나를 처리할 때 | `OrderCancellationHandler`, `PaymentFailureHandler` | 단순 변환이나 검증 |
| `Processor` | 입력을 처리하거나 변환한 뒤 후속 작업을 할 때 | `RefundProcessor`, `OrderItemProcessor` | 책임이 불분명한 범용 접미사 |
| `Manager` | 여러 자원, 상태, 객체의 수명주기와 조정을 담당할 때 | `ReservationHoldManager`, `InventoryReservationManager` | 단일 규칙, 계산, 생성 |
| `Allocator` | 한정된 자원이나 수량을 배분하거나 점유할 때 | `InventoryAllocator`, `CouponQuotaAllocator` | 배분 의미 없는 단순 재고 감소 |
| `Calculator` | 입력으로 결정적인 수치, 금액, 점수를 계산할 때 | `OrderPriceCalculator`, `DeliveryFeeCalculator` | 외부 상태를 변경하거나 정책을 선택할 때 |
| `Mapper` | A 타입을 B 타입으로 변환할 때 | `OrderItemMapper`, `PaymentResultMapper` | 규칙을 판단하거나 상태를 변경할 때 |
| `Provider` | 필요한 값이나 객체를 제공할 때 | `ExchangeRateProvider`, `CurrentUserProvider` | 생성 규칙이나 복잡한 선택 로직 |
| `Publisher` | 도메인 이벤트나 외부 메시지를 발행할 때 | `OrderCreatedEventPublisher` | 이벤트를 처리할 때 |
| `Synchronizer` | 둘 이상의 상태를 맞출 때 | `OrderStatusSynchronizer`, `InventorySynchronizer` | 일반 업데이트 |
| `Coordinator` | 여러 `internal` 작업의 순서를 조정할 때 | `CheckoutCoordinator` | `service`가 이미 유스케이스 흐름을 조정할 때 |

## 이름 예시

### 주문 생성

`OrderService`는 다음 세부 작업을 사용합니다.

```text
OrderService
├── OrderFactory
├── OrderNumberGenerator
├── InventoryAllocator
├── CouponEligibilityValidator
├── OrderPriceCalculator
└── OrderCreatedEventPublisher
```

| 클래스 | 책임 |
| --- | --- |
| `OrderFactory` | 주문 Aggregate를 생성합니다. |
| `OrderNumberGenerator` | 신규 주문 번호를 생성합니다. |
| `InventoryAllocator` | 주문 항목별 재고를 예약하고 할당합니다. |
| `CouponEligibilityValidator` | 주문에서 쿠폰을 사용할 수 있는지 검증합니다. |
| `OrderPriceCalculator` | 상품 금액, 할인 금액, 배송비를 계산합니다. |
| `OrderCreatedEventPublisher` | 주문 생성 이벤트를 발행합니다. |

```java
@Service
public class OrderService {

    public OrderId create(CreateOrderCommand command) {
        couponEligibilityValidator.validate(command.coupon(), command.items());

        InventoryAllocation allocation = inventoryAllocator.allocate(command.items());
        OrderPrice price = orderPriceCalculator.calculate(command, allocation);
        Order order = orderFactory.create(command, price, orderNumberGenerator.generate());

        orderRepository.save(order);
        orderCreatedEventPublisher.publish(order);

        return order.getId();
    }
}
```

### 주문 취소

`OrderCancellationService`는 다음 세부 작업을 사용합니다.

```text
OrderCancellationService
├── OrderCancellationValidator
├── InventoryReservationManager
├── PaymentRefundHandler
├── OrderStatusTransitionHandler
└── OrderCancelledEventPublisher
```

| 클래스 | 책임 |
| --- | --- |
| `OrderCancellationValidator` | 주문 취소 가능 여부와 정책 위반을 검증합니다. |
| `InventoryReservationManager` | 기존 재고 예약을 해제합니다. |
| `PaymentRefundHandler` | 결제 수단별 환불을 처리합니다. |
| `OrderStatusTransitionHandler` | 주문 상태를 취소 상태로 변경합니다. |
| `OrderCancelledEventPublisher` | 주문 취소 이벤트를 발행합니다. |

`InventoryReservationManager`는 예약 생성과 해제를 함께 관리합니다. 따라서 상태 수명주기를 나타내는 `Manager`를 사용합니다.

`PaymentRefundHandler`는 환불 명령 하나를 처리합니다. 따라서 `Handler`를 사용합니다.

### 결제 승인

`PaymentApprovalService`는 다음 세부 작업을 사용합니다.

```text
PaymentApprovalService
├── PaymentMethodResolver
├── PaymentAmountValidator
├── PaymentGatewayProvider
├── PaymentApprovalHandler
├── PaymentResultMapper
└── PaymentStatusTransitionHandler
```

| 클래스 | 책임 |
| --- | --- |
| `PaymentMethodResolver` | 요청과 정책에 맞는 결제 수단을 결정합니다. |
| `PaymentAmountValidator` | 결제 가능 금액과 주문 금액이 일치하는지 검증합니다. |
| `PaymentGatewayProvider` | 결제 수단에 맞는 외부 PG 연동 객체를 제공합니다. |
| `PaymentApprovalHandler` | 외부 PG 승인 요청을 처리합니다. |
| `PaymentResultMapper` | 외부 PG 응답을 도메인 결과로 변환합니다. |
| `PaymentStatusTransitionHandler` | 결제 상태를 승인 또는 실패로 변경합니다. |

### 쿠폰 적용

`CouponApplicationService`는 다음 세부 작업을 사용합니다.

```text
CouponApplicationService
├── CouponEligibilityValidator
├── CouponDiscountCalculator
├── CouponQuotaAllocator
├── CouponUsageManager
└── CouponAppliedEventPublisher
```

| 클래스 | 책임 |
| --- | --- |
| `CouponEligibilityValidator` | 쿠폰 기간, 대상, 최소 주문 금액 조건을 검증합니다. |
| `CouponDiscountCalculator` | 할인 금액을 계산합니다. |
| `CouponQuotaAllocator` | 선착순 또는 한정 수량 쿠폰의 사용 가능 수량을 확보합니다. |
| `CouponUsageManager` | 쿠폰 사용, 취소, 복구 상태를 관리합니다. |
| `CouponAppliedEventPublisher` | 쿠폰 적용 이벤트를 발행합니다. |

`CouponUsageManager`는 쿠폰 사용 상태의 생성, 취소, 복구를 관리합니다. 따라서 상태 수명주기를 나타내는 `Manager`를 사용합니다.

## 메서드 이름

메서드 이름은 `동사 + 대상 + 보충 정보`로 작성합니다.

클래스 이름과 메서드 이름에서 같은 책임을 반복하지 않습니다.

```java
// 비권장
orderNumberGenerator.generateOrderNumber();
couponEligibilityValidator.validateCouponEligibility();
inventoryAllocator.allocateInventory();

// 권장
orderNumberGenerator.generate();
couponEligibilityValidator.validate();
inventoryAllocator.allocate();
```

### Service의 private 메서드

Service의 public 메서드는 유스케이스 처리 순서를 직접 보여줍니다. 코드 길이를 줄이거나 중복을 제거하기 위한 목적만으로 유스케이스 단계를 private 메서드로 추출하지 않습니다. 짧고 익숙한 코드의 중복이 흐름을 더 읽기 쉽게 만든다면 중복을 허용합니다.

추출할 로직이 검증, 변환, 조회, 알림, 외부 호출처럼 독립된 업무 책임을 가지면 `service/internal` 또는 역할에 맞는 영역의 클래스로 분리합니다.

`internal`, Repository, 외부 연동 클래스는 하나의 책임을 구현하기 위한 계산과 기술 세부 사항을 private 메서드로 분리할 수 있습니다. private 메서드가 별도의 업무 책임을 가지기 시작하면 독립된 클래스로 분리합니다.

### 기본 형태

```java
calculateDiscount(order);
validateCancellation(order);
resolvePaymentMethod(command);
findAvailableCoupon(memberId);
reserveInventory(orderItems);
releaseReservation(orderId);
publishOrderCreated(order);
```

| 형태 | 예시 | 사용하는 경우 |
| --- | --- | --- |
| `verb()` | `generate()`, `validate()`, `resolve()` | 클래스 책임이 충분히 구체적일 때 |
| `verb(target)` | `calculateDiscount()`, `releaseReservation()` | 대상을 구분해야 할 때 |
| `verbBy(criteria)` | `findById()`, `resolveByCountry()` | 조회나 결정 기준을 드러낼 때 |
| `verbIf(condition)` | `reserveIfAvailable()` | 조건부 실행이 메서드 계약의 핵심일 때 |
| `canVerb()` | `canCancel()`, `canUse()` | 가능 여부를 `boolean`으로 반환할 때 |
| `is...()`, `has...()`, `should...()` | `isExpired()`, `hasStock()`, `shouldRetry()` | 상태나 조건을 판단할 때 |

### 조회 메서드

Spring Data JPA의 동적 쿼리 문법과 반환 계약에 맞춰 조회 메서드 이름을 정합니다.

| 상황 | 권장 이름 | 반환 예시 |
| --- | --- | --- |
| 반드시 존재해야 함 | `getById()`, `getRequiredById()` | `Order` |
| 없을 수 있음 | `findById()` | `Optional<Order>` |
| 존재 여부만 필요 | `existsById()` | `boolean` |
| 여러 건 조회 | `findAllByMemberId()` | `List<Order>` |
| 조건에 맞는 하나를 결정 | `resolve()` | `PaymentMethod` |

```java
// 없으면 도메인 예외를 발생시킵니다.
Order order = orderReader.getById(orderId);

// 없을 수 있으므로 Optional을 반환합니다.
Optional<Order> order = orderReader.findByOrderNumber(orderNumber);
```

### 상태 변경 메서드

상태를 바꿀 때는 구현 동사 대신 도메인 동사를 사용합니다.

```java
// 비권장
order.updateStatus(CANCELLED);
payment.updateStatus(APPROVED);
coupon.updateUsed(true);

// 권장
order.cancel();
payment.approve();
coupon.use();
```

외부 요인이나 정책으로 상태를 바꾸면 원인을 메서드 이름에 드러냅니다.

```java
order.cancelByMember(memberId);
order.cancelBySeller(sellerId);
payment.fail(failureReason);
reservation.expire();
coupon.restore();
```

`update`, `modify`, `change`처럼 변경 대상을 알 수 없는 동사는 피합니다.

```java
// 비권장
order.changeStatus(status);
member.updateProfile(command);

// 권장
order.confirm();
order.startDelivery();
member.changeNickname(nickname);
member.changeProfileImage(profileImage);
```

## 조건과 분기 표현

실행 흐름을 압축해 숨기지 않습니다. 조건에 따라 값이 달라지면 3항 연산자 대신 `if`로 분기를 드러냅니다.

```java
// 피할 예시
String field = parameterName == null ? "request" : parameterName;

// 권장 예시
String field = parameterName;
if (field == null) {
    field = "request";
}
```

한 분기는 한 업무 결정을 표현합니다. 논리 연산자의 개수를 기계적으로 제한하지는 않습니다. 다만 독립적으로 바뀌는 권한, 상태, 예외 조건을 호출 지점에 길게 조합하지 않습니다. 결정 자체에 이름과 책임이 필요하면 [도메인 경계](../architecture/domain-boundaries.md)에 맞는 Validator 또는 정책 객체에서 판단합니다.

```java
rejoinPolicy.ensureRejoinAllowed(user, command, now);
```

매번 같은 업무 오류를 던지는 `isBlocked()`의 결과를 호출자가 다시 해석하기보다, 거절 결과를 표현하는 `ensureRejoinAllowed()`처럼 이름을 정할 수 있습니다. 실제 오류 소유자는 [Error Handling](error-handling.md)을 따릅니다.

## 상수

문자열이나 숫자를 분리했다는 이유만으로 상수를 만들지 않습니다. 다음처럼 이름이 값보다 더 많은 의미를 전달할 때 상수로 둡니다.

- 업무 규칙의 기준값
- 프로토콜의 header와 field 이름
- 보안 또는 검증 정책
- 여러 곳에서 반드시 함께 바뀌어야 하는 계약

한 메서드에서 한 번만 사용하고 값 자체가 분명한 literal은 그대로 둘 수 있습니다. 단순 중복 제거용 상수나 원래 값을 그대로 읽어 주는 이름은 피합니다.

## Private helper method

호출 메서드에서 중요한 조건과 데이터 흐름이 보여야 합니다. 코드 길이를 줄이거나 중복만 없애려고 private 메서드를 만들지 않습니다. 추출할 때는 다음을 함께 확인합니다.

1. 분리한 부분에 독립적인 책임과 이름이 있는가?
2. 추출 뒤에도 호출 메서드에서 주요 순서와 조건을 파악할 수 있는가?
3. 실제 반복이나 다른 변경 이유가 확인되었는가?

Service의 유스케이스 단계는 위 [Service의 private 메서드](#service의-private-메서드) 기준을 우선합니다. `internal`, Repository와 외부 연동 클래스의 기술 세부 사항은 같은 책임 안에서 private 메서드로 나눌 수 있습니다. 새 업무 책임이 생기면 해당 owner의 클래스로 분리합니다.

## 메서드 인자와 중간값

메서드 인자 안에 다른 호출이나 계산을 중첩하지 않습니다. 결과의 의미가 드러나는 지역 변수에 담아 호출 순서를 보여줍니다.

```java
// 피할 예시
tokenStore.save(user.getId(), new AccessToken(token, clock.instant().plus(ttl)));

// 권장 예시
Long userId = user.getId();
Instant expiresAt = clock.instant().plus(ttl);
AccessToken accessToken = new AccessToken(token, expiresAt);
tokenStore.save(userId, accessToken);
```

단순 literal, 상수와 이미 준비한 변수는 그대로 전달할 수 있습니다. 중간 변수의 이름은 계산 방법보다 값의 의미를 설명해야 합니다.

## 여러 값을 반환하는 타입과 DTO

의미가 다른 여러 값을 `Map<String, Object>`, 배열 순서나 runtime cast로 전달하지 않습니다. 값의 owner가 드러나는 이름 있는 타입을 사용합니다.

| 값의 의미 | 위치 |
| --- | --- |
| 상태와 불변식이 있는 업무 개념 | `data`의 Entity 또는 Value Object |
| 유스케이스 실행 결과 | `service`가 소유하는 결과 타입 |
| 목록과 집계의 조회 결과 | `repository`의 Projection 또는 Result |
| HTTP 요청과 응답 | `web/dto/request`, `web/dto/response` |

한 메서드에서만 쓴다는 이유로 `service/internal` 클래스 안에 임시 중첩 `record`를 만들지 않습니다. 최상위 HTTP response DTO는 별도 파일에 둡니다. 단순하고 불변인 DTO에는 `record`를 사용할 수 있지만 기존 class DTO를 형식만 맞추려고 바꾸지 않습니다.

응답 DTO의 순수한 `from` 메서드는 이미 계산된 Service 결과를 HTTP body로 옮길 수 있습니다. 여기에서 Repository, 외부 API, `Clock`, cookie, header를 호출하거나 업무 규칙을 판단하지 않습니다. cookie와 header 조합은 Controller가 담당합니다.

```java
public record PostSummaryResponse(Long id, String title) {

    public static PostSummaryResponse from(PostSummaryResult result) {
        return new PostSummaryResponse(result.id(), result.title());
    }
}
```

이 코드는 타입의 경계를 보여주는 예시이며 현재 구현을 복사한 것은 아닙니다. HTTP JSON 구조에서만 의미가 있는 중첩 DTO는 바깥 DTO 안에 둘 수 있습니다. 독립적인 업무 개념, 생명주기 또는 불변식을 갖거나 다른 경계에서 쓰이면 별도 타입의 owner를 먼저 정합니다. Controller 내부에 최상위 응답 DTO를 선언하지 않고, HTTP JSON 구조를 `data`의 Value Object에 강요하지 않습니다.

## 반복과 분기

`Stream`을 습관적으로 사용하지 않습니다. 단순 filter, map, aggregate처럼 위에서 아래로 한 번에 읽히면 사용할 수 있습니다. 분기, 상태 변경, 예외 처리 또는 여러 중간 단계가 있으면 `if`, `for`, `switch`를 우선합니다. `Stream` 안에서 외부 상태를 바꾸거나 중첩된 `Stream`과 `flatMap`으로 반복 구조를 숨기지 않습니다.

## Null, fallback, 예외와 retry

`null` 가능성은 값의 출처와 계약에서 확인합니다. 발생 경로를 설명할 수 없는 `null`을 가정해 같은 검사를 반복하지 않습니다.

- HTTP 요청과 외부 provider 응답은 들어오는 경계에서 검증합니다.
- Bean Validation을 통과한 입력과 non-null 내부 인자는 다시 검사하지 않습니다.
- JPA의 non-null 매핑과 DB `NOT NULL` 계약이 보장하는 값을 추측으로 다시 검사하지 않습니다.
- 조회 부재는 `Optional`이나 해당 유스케이스의 업무 실패로 표현합니다. Repository가 업무 예외를 만들지는 않습니다.

깨진 내부 불변식을 빈 문자열, 빈 목록, 임의의 enum이나 현재 시각으로 바꾸어 계속 진행하지 않습니다. fallback은 계약에 정의되어 있고 호출자가 원래 실패와 구분할 수 있을 때만 사용합니다.

`try/catch`는 예상한 예외를 현재 경계가 복구하거나 업무 실패로 해석할 수 있을 때만 둡니다. 넓은 catch로 원인을 숨기거나 성공 값으로 바꾸지 않습니다. retry는 일시적 실패, 멱등성, 횟수 제한과 이미 일어난 외부 효과를 설명할 수 있을 때만 추가합니다. 자세한 오류 책임은 [Error Handling](error-handling.md)을 따릅니다.

## 코드 검토 기준

- 조건, 실패 지점과 실행 순서를 호출 메서드에서 바로 확인할 수 있는가?
- 상수나 helper를 따라가야만 실제 값과 조건을 알 수 있지는 않은가?
- 중복 제거가 변경 이유가 다른 책임을 억지로 묶지는 않았는가?
- `Stream`이 반복과 부수 효과의 순서를 감추지 않는가?
- 여러 반환값의 의미와 owner가 타입으로 드러나는가?
- DTO, Service 결과, 조회 Projection, Value Object의 경계가 구분되는가?
- 근거 없는 `null` 방어, fallback이나 넓은 catch가 원래 실패를 숨기지 않는가?

테스트의 선택, 이름, DCI 구조와 Given, When, Then 기준은 [Testing](testing.md)을 따른다.
