# Controller 분리 기준

HTTP API의 Controller 구성과 분리 시점을 정할 때 적용합니다. Service 구성은 [서비스 분리 기준](service-boundaries.md), Web 계층의 책임과 DTO 위치는 [Domain Boundaries](domain-boundaries.md#web-계층)를 따릅니다.

## 기본 단위와 이름

- Controller는 업무 자원과 API 기능을 기준으로 묶습니다. 같은 기능의 조회와 변경은 함께 둡니다.
- 기본 이름은 `[대상]Controller`, 독립 기능을 분리하면 `[기능명]Controller`를 사용합니다.
- Service마다 Controller를 만들거나 Query와 Command에 맞춰 Controller를 기계적으로 분리하지 않습니다.
- 하나의 Controller가 같은 API 기능에 필요한 여러 Service를 호출할 수 있습니다.
- 내부 협력 객체, 스케줄러와 독립 HTTP API가 없는 기능을 위해 Controller를 추가하지 않습니다.
- 기존 영역의 명명과 Swagger 주석 방식을 유지합니다. 일괄적인 클래스 이동은 별도 리팩터링으로 다룹니다.

## 분리 판단

분리할 API 묶음이 다음 두 조건을 모두 만족하면 별도 Controller로 분리합니다.

1. 임시저장, 번역, 통계처럼 독립적인 호출 목적과 기능 이름을 가집니다.
2. 함께 두어서 다음 중 하나의 문제가 실제로 발생합니다.
   - 기능별로 사용하는 Service와 요청, 응답 타입이 갈려 무관한 API를 함께 확인해야 합니다.
   - 서로 다른 기능의 API 추가와 계약 변경이 반복적으로 같은 클래스에 집중됩니다.
   - 독립적인 권한 요구나 호출 절차를 가진 API 묶음을 한 클래스에서 관리하기 어렵습니다.

줄 수, 엔드포인트 수, HTTP 메서드와 Service 개수만으로 분리하지 않습니다. Swagger 설명 때문에 파일이 길어진 경우에는 API 기능의 책임이 나뉘는지 먼저 확인합니다. 인증 여부가 다른 API도 같은 기능이면 함께 둘 수 있습니다.

## Service 연결과 책임

- Controller는 HTTP 입력, 요청 형식 검증, 인증 정보 전달과 응답 변환을 담당합니다.
- 업무 검증, Repository 접근, 트랜잭션, 외부 호출과 상태 변경의 순서는 Service에 둡니다.
- 여러 상태 변경을 하나의 업무 흐름으로 수행해야 한다면 그 흐름을 담당하는 Service를 호출합니다. Controller에 업무 오케스트레이션을 추가하지 않습니다.
- 일반 게시글 조회와 변경을 함께 받는 Controller는 `PostService`와 `PostQueryService`를 직접 호출할 수 있습니다. 전달만 담당하는 Service를 추가하지 않습니다.

## 게시글 적용 예시

아래 표는 권장 배치 예시이며, 현재 구현과 다른 항목은 별도 리팩터링 대상입니다.

| Controller | API 기능 | 호출 대상 |
| --- | --- | --- |
| `PostController` | 일반 게시글 생성, 수정, 삭제, 상태와 반경 변경, 상세, 목록, 검색, 공유와 유사 글 조회 | `PostService`, `PostQueryService`, `PostDetailService` |
| `TemporaryPostController` | 임시저장 생성, 조회와 삭제 | `TemporaryPostService` |
| `PostTranslationController` | 게시글 번역 요청 | `PostTranslationService` |
| `PostStatisticsController` | 게시글 통계 조회 | `PostStatisticsService` |
| `PostFavoriteController` | 찜 등록, 취소와 내 찜 목록 | 찜 기능의 변경 및 조회 Service |

인기 선정이 일반 목록의 내부 처리이면 `HotPostController`를 만들지 않습니다. 독립적인 인기 목록 API 하나는 `PostController`에 둘 수 있고, 인기 API 묶음이 분리 조건을 만족하면 `HotPostController`를 검토합니다. 이미지가 게시글 생성과 수정의 일부이면 `PostImageController`를 만들지 않습니다.

현재 [PostController](../../src/main/java/com/fmi/domain/post/post/web/controller/PostController.java)는 일반 게시글 API를, [TemporaryPostController](../../src/main/java/com/fmi/domain/post/post/web/controller/TemporaryPostController.java)와 [PostTranslationController](../../src/main/java/com/fmi/domain/post/post/web/controller/PostTranslationController.java)는 임시저장과 번역 API를 담당합니다. [PostStatisticsController](../../src/main/java/com/fmi/domain/post/post/web/controller/PostStatisticsController.java)는 통계 API를 담당합니다. [PostFavoriteController](../../src/main/java/com/fmi/domain/post/post/web/controller/PostFavoriteController.java)는 찜 등록, 취소와 목록 조회에 `PostFavoriteService`를 사용합니다.

## 다른 업무의 적용 예시

현재 구현의 기능별 구성입니다. 같은 수의 Controller를 만들라는 기준은 아닙니다.

| 업무 | Controller 구성 |
| --- | --- |
| 관리자 | `AdminInquiryController`, `AdminReportController`, `AdminUserController`, `AdminNoticeController`, `AdminSignupController`, `AdminPostController` |
| 회원 | `UserController`, `UserActivityController`, `UserLanguageController`, `UserImageController` |
| 인증 | `AuthController`, `PasswordController`, `WithdrawalController` |
| 알림 | `NotificationController`, `NotificationSettingsController`, `PushSubscriptionController` |
| 공지 | `NoticeController`, `NoticeLikeController`, `NoticeCommentController`, `NoticeCommentLikeController` |

회원 차단 API는 소유 업무의 [BlockController](../../src/main/java/com/fmi/domain/user/block/web/controller/BlockController.java)에 둡니다. 패키지 이동 후에도 기존 `/reports` 경로와 `Report` 태그를 유지합니다.

## URL, 인증과 Swagger

- Controller 클래스와 URL 계층을 별도로 결정합니다. 여러 Controller가 `/posts` 아래의 서로 다른 경로를 담당할 수 있습니다.
- 클래스 이동만 하는 작업에서는 HTTP 메서드, 경로, 인증과 권한, 요청과 응답, 상태 코드, 오류 계약을 유지합니다.
- 경로를 나눌 때 정적 경로와 `/{id}` 같은 변수 경로, 클래스 수준과 메서드 수준 매핑의 조합에 의한 충돌을 확인합니다.
- 이동하는 API에 적용된 클래스 수준의 보안, 검증과 기타 공통 애너테이션을 확인하고 기존 동작을 유지합니다.
- Controller 이름에 맞추기 위해 Swagger 태그를 새로 만들지 않습니다. 태그와 분류 변경은 [OpenAPI Guide](../contributing/openapi-guide.md#분류와-이름)를 따릅니다.
- 실제 계약을 변경하면 [API Pattern](../contributing/api-pattern.md)에 따라 합의하고 구현, 테스트와 API 문서를 함께 반영합니다.

## 분리 검증

기존 API를 이동할 때는 [Refactoring](../contributing/refactoring.md)을 따르고, 자동 테스트의 범위는 [Testing의 테스트 범위](../contributing/testing.md#테스트-범위)를 따릅니다. 코드 변경 검토에서 이동 전후의 매핑, 인증과 권한, 입력 검증과 응답 계약이 유지되는지 확인하고, OpenAPI에서 API 누락과 중복을 확인합니다. 문서만 변경한 경우 실제 API 동작이 바뀌었다고 설명하지 않습니다.
