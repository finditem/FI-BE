# 서비스 분리 기준

Service의 책임과 분리 시점을 정할 때 적용합니다. 패키지와 계층별 책임은 [Domain Boundaries](domain-boundaries.md), 기존 코드의 변경 범위는 [AGENTS.md](../../AGENTS.md#레거시-코드와-리팩터링)를 따릅니다.

## 기본 단위

- Service는 응집된 업무 기능 단위로 구성합니다. 같은 기능의 조회와 변경은 함께 둘 수 있습니다.
- 모든 도메인에 Query와 Command 분리를 강제하지 않습니다. 도메인마다 같은 클래스 수를 맞추지 않습니다.
- 이미 분리된 Service를 이 기준에 맞추기 위해 일괄 통합하거나 재분리하지 않습니다.
- 줄 수, 메서드 수와 SQL 종류만으로 Service를 분리하지 않습니다.

## 분리 판단

분리할 메서드 묶음이 다음 두 조건을 모두 만족하면 별도 Service로 분리합니다.

1. 입력, 결과와 처리 흐름을 독립적인 업무 책임으로 설명하고 이름 붙일 수 있습니다.
2. 함께 두어서 다음 중 하나의 문제가 실제로 발생합니다.
   - 메서드 묶음별로 사용하는 의존성이 갈리고, 변경할 때 서로 무관한 책임을 함께 확인해야 합니다.
   - 서로 다른 기능의 변경이 반복적으로 같은 클래스에 집중됩니다.
   - 테스트에서 다른 기능만 사용하는 의존성까지 반복적으로 준비해야 합니다.

클래스를 나누기 전에 긴 메서드, 쿼리 구성, 형식 변환이 해당 계층의 책임에 맞게 배치되어 있는지 확인합니다. 분리 근거는 변경 보고에 책임과 실제 문제로 적습니다.

| 상태 | 선택 |
| --- | --- |
| 같은 기능의 조회와 변경이 작고 함께 이해하기 쉽습니다. | 하나의 기능 Service로 유지합니다. |
| 검색 조건, 정렬, 페이징, 집계와 응답 조합이 커지고 변경 흐름과 독립적인 책임을 이룹니다. | 해당 기능의 QueryService를 분리합니다. |
| 임시저장, 번역, 토큰 관리처럼 별도 업무 흐름이 분리 조건을 만족합니다. | 기능명 Service로 분리하고 그 안의 조회와 변경은 함께 둘 수 있습니다. |
| 생성, 수정, 삭제가 같은 상태와 검증 규칙을 공유합니다. | 동작별 Service를 기계적으로 만들지 않습니다. |

## 이름과 조회 경계

- 기본 이름은 `[대상]Service` 또는 `[기능명]Service`로 정합니다.
- 조회를 분리하면 `[대상 또는 기능명]QueryService`를 사용하고, 변경은 기존 Service에 둡니다. 대칭을 맞추기 위한 CommandService는 추가하지 않습니다. 기존 영역의 명명 방식은 유지합니다.
- 조회는 해당 업무 기능에 둡니다. 임시저장 조회를 일반 게시글 QueryService로, 번역 조회를 일반 조회 Service로 모으지 않습니다.
- 변경에 필요한 Entity 조회, 잠금과 검증은 변경 유스케이스에 둡니다. SELECT라는 이유로 QueryService를 경유하지 않습니다.
- 재사용할 Entity 조회가 필요하다는 이유만으로 API 조회 Service를 공통 Reader로 사용하지 않습니다. 공통 조회 책임의 추출은 [Domain Boundaries](domain-boundaries.md#팀-논의가-필요한-규칙)를 따릅니다.
- QueryService라는 이름만 보고 `readOnly = true`를 적용하지 않습니다. 조회수 기록, 번역 생성, Redis 쓰기와 같은 부수 효과가 있는지 확인합니다.

## 트랜잭션과 협력 객체

- 유스케이스를 담당하는 Service가 처리 순서와 트랜잭션 경계를 소유합니다.
- 분리 전후의 원자성, 잠금 순서, 외부 호출 순서와 실패 뒤 상태를 확인합니다. 별도 Service로 옮겼다는 이유로 새 트랜잭션이나 비동기 처리를 도입하지 않습니다.
- Validator, Recorder와 같은 협력 객체는 독립 API 기능 Service와 구분합니다. 기존 Service를 새 Service에 요청을 전달하기만 하는 중계 계층으로 남기지 않습니다.
- Service 분리가 Repository 분리, 별도 DB, 이벤트 동기화 도입을 요구하지는 않습니다. Repository의 구성은 [Repository 분리 기준](repository-boundaries.md), 데이터 접근 방식은 [Persistence](persistence.md)를 따릅니다.

## 게시글 적용 예시

아래는 기존 클래스의 이름을 사용한 책임 배치 예시입니다. 현재 모든 메서드의 배치와 일치한다는 의미는 아닙니다.

| 클래스 | 책임 |
| --- | --- |
| `PostService` | 일반 게시글 생성, 수정, 삭제, 상태와 반경 변경 |
| `PostQueryService` | 일반 게시글 상세, 목록, 검색, 공유와 유사 게시글 조회 |
| `PostDetailService` | 상세 조회 완료 후 관련 알림 읽음 처리 조정 |
| `TemporaryPostService` | 임시저장 생성, 조회와 삭제 |
| `HotPostService` | 인기 게시글 후보 조회와 선정 |
| `PostTranslationService` | 번역 조회와 생성, 기존 번역 무효화 |
| `PostStatisticsService` | 게시글 통계 조회 |
| `PostImageService` | 이미지 업로드, 삭제, 썸네일 지정과 이미지 조회 |

`PostStatisticsRecorder`는 통계 기록, `PostUpdateRateLimiter`는 수정 빈도 제한, `PostValidator`는 업무 검증을 담당하는 협력 객체로 구분합니다. `PostCleanupScheduler`는 정리 작업의 실행 진입점입니다.

현재 구현은 [PostService](../../src/main/java/com/fmi/domain/post/post/service/PostService.java), [PostQueryService](../../src/main/java/com/fmi/domain/post/post/service/PostQueryService.java), [TemporaryPostService](../../src/main/java/com/fmi/domain/post/post/service/TemporaryPostService.java)에서 확인합니다. `PostQueryService.getPost()`에는 Redis 조회 기록과 DB 조회수 증가가 포함되어 있습니다. 이 흐름을 분리할 때는 순수 조회로 간주하지 않고 실패 처리와 트랜잭션을 함께 검토합니다.

찜 등록, 취소와 내 찜 목록은 [PostFavoriteService](../../src/main/java/com/fmi/domain/post/post/service/PostFavoriteService.java)에서 찜 기능으로 묶습니다. 목록이 분리 조건을 만족하면 찜 QueryService로 분리합니다.

## 다른 업무의 적용 예시

현재 구현에서 의존성이 다른 업무 흐름을 분리한 사례입니다. 클래스 수와 조회·변경 분리를 모든 업무에 일괄 적용하지 않습니다.

| 업무 | 책임 배치 |
| --- | --- |
| 관리자 | `AdminInquiryService`, `AdminReportQueryService`, `AdminUserQueryService`, `AdminPostQueryService`가 담당 업무의 조회와 처리를 소유합니다. |
| 회원 | `UserService`는 내 프로필과 약관, `UserProfileQueryService`는 공개 프로필, `UserActivityQueryService`는 활동 목록, `UserLanguageService`는 언어 설정, `UserImageService`는 이미지 업로드를 담당합니다. |
| 장소 | `PlaceService`는 관리 상세와 상태 변경, `PlaceHomeQueryService`, `PlaceMapQueryService`, `PlaceQueryService`는 홈, 지도와 장소 요약 조회를 담당합니다. |
| 알림 | `NotificationService`는 수신함, `NotificationSettingsService`는 설정, `NotificationDeliveryService`는 개별 저장·전송, `NotificationBroadcastService`는 발송 대상 선정, `NotificationSubscriptionService`는 SSE 연결과 미읽은 알림 재전송을 담당합니다. |
| 문의·신고 | `InquiryQueryService`, `ReportQueryService`는 목록과 상세 조회를 담당하며 생성과 상태 변경은 각 기본 Service에 둡니다. |
| 채팅 | `ChatRoomQueryService`, `ChatMessageQueryService`는 방과 메시지 조회를 담당하며 생성, 퇴장과 메시지 읽음 처리는 각 기본 Service에 둡니다. |

[PostDetailService](../../src/main/java/com/fmi/domain/post/post/service/PostDetailService.java), [InquiryDetailService](../../src/main/java/com/fmi/domain/inquiry/inquiry/service/InquiryDetailService.java), [NoticeDetailService](../../src/main/java/com/fmi/domain/notice/notice/service/NoticeDetailService.java)는 상세 조회와 후속 알림 읽음 처리를 조정합니다. 기존 동작을 유지하기 위해 상세 조회 트랜잭션이 종료된 뒤 알림을 별도 트랜잭션에서 처리합니다. 조정 Service에 전체를 묶는 트랜잭션을 추가하지 않습니다.

## Controller와의 관계

Controller의 기능별 구성, 분리 시점과 Service 연결은 [Controller 분리 기준](controller-boundaries.md)을 따릅니다.

## 팀 논의가 필요한 규칙

새 도메인 분리, `internal` 도입, Port 선택과 업무 정책 변경은 기존 [팀 논의 기준](domain-boundaries.md#팀-논의가-필요한-규칙)과 [미확정 정책 처리 절차](../../AGENTS.md#문서에-없는-정책)를 따릅니다. 기존 흐름의 분리는 [Refactoring](../contributing/refactoring.md)에 따라 진행합니다.
