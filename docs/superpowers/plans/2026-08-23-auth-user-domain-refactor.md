# auth·user 도메인 행위 리팩터링 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** auth·user 범위의 Setter 기반 상태 변경을 `User` 도메인 행위와 읽기 쉬운 유스케이스 Service로 전환한다.

**Architecture:** `User`는 상태 일관성을 지키는 행위를 제공하고, Service는 Repository 조회·유스케이스 검증·외부 효과의 순서를 담당한다. 비밀번호 정책과 암호화처럼 인증 유스케이스에 반복되는 협력만 `auth/service/internal`에 둔다.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, JUnit 5, Mockito

**Spec:** `docs/superpowers/specs/2026-08-23-auth-user-domain-design.md`

## Global Constraints

- 기준 브랜치는 `refactor/#528`이고 PR 대상은 `develop`이다.
- API 경로·응답, DB 컬럼, Redis와 외부 호출 순서를 변경하지 않는다.
- 새 production 행위는 먼저 실패하는 순수 단위 테스트를 작성한다.
- `Query` 계층을 만들지 않고, `internal`은 중복 또는 복잡성이 확인된 경우에만 만든다.

### Task 1: User 비밀번호 상태 전이

**Files:**
- Modify: `src/main/java/com/fmi/domain/user/data/User.java`
- Create: `src/test/java/com/fmi/domain/user/data/UserTest.java`

**Interfaces:** `issueTemporaryPassword(String, LocalDateTime, LocalDateTime)`, `restoreExpiredTemporaryPassword(LocalDateTime)`, `changePassword(String, LocalDateTime)`

- [ ] Write a failing test that verifies the original password is preserved only on the first temporary-password issue.
- [ ] Run `./gradlew test --tests com.fmi.domain.user.data.UserTest` and verify that the missing domain method causes RED.
- [ ] Implement the smallest User transition needed for that test, then repeat RED–GREEN for expiry restoration and password change.
- [ ] Run `./gradlew test --tests com.fmi.domain.user.data.UserTest` and commit `refactor: User 비밀번호 상태 전이 캡슐화`.

### Task 2: User 계정 상태 전이

**Files:**
- Modify: `src/main/java/com/fmi/domain/user/data/User.java`
- Modify: `src/test/java/com/fmi/domain/user/data/UserTest.java`

**Interfaces:** `markEmailVerified()`, `agreeTerms(...)`, `changeNickname(String, LocalDateTime)`, `changeProfileImage(String, LocalDateTime)`, `removeProfileImage(LocalDateTime)`, `recordProfileUpdate(LocalDateTime)`, `withdraw(...)`, `reactivateForSocialLogin()`

- [x] Add failing tests for each isolated state transition, implement the minimal domain method, and verify the focused test after every change.
- [x] Replace the matching setter calls in auth·user Services while preserving the existing external-effect order.
- [x] Commit `refactor: User 계정 상태 전이 캡슐화`.

### Task 3: 인증 Service와 내부 비밀번호 협력자

**Files:**
- Create: `src/main/java/com/fmi/domain/auth/service/internal/PasswordPolicy.java`
- Create: `src/main/java/com/fmi/domain/auth/service/internal/PasswordCodec.java`
- Modify: `AuthService`, `PasswordService`, `PasswordResetService`, `TemporaryPasswordCleanupScheduler`, `SocialLoginService`, `WithdrawalService`
- Test: `src/test/java/com/fmi/domain/auth/service/*Test.java`

- [ ] Add failing Service tests for policy validation and User domain-method usage.
- [ ] Add only the repeated PasswordPolicy and PasswordCodec collaborators; replace direct User setters.
- [ ] Run the affected auth tests and commit `refactor: auth 비밀번호 유스케이스 책임 정리`.

### Task 4: AuthController 통합

**Files:**
- Modify: `src/main/java/com/fmi/domain/auth/web/controller/AuthController.java`
- Delete: `src/main/java/com/fmi/domain/auth/web/controller/AccountAuthController.java`
- Modify: `src/test/java/com/fmi/domain/auth/web/controller/AuthControllerTest.java`
- Delete: `src/test/java/com/fmi/domain/auth/web/controller/AccountAuthControllerTest.java`

- [ ] Add failing AuthController tests for the existing password verify, password change, and withdrawal routes.
- [ ] Move the existing methods with unchanged mappings and response wrappers, run all auth controller tests, and commit `refactor: AuthController에 계정 인증 API 통합`.

### Task 5: UserQueryService 의존 제거

**Files:**
- Modify: all callers of `com.fmi.service.UserQueryService`
- Delete: `src/main/java/com/fmi/service/UserQueryService.java`
- Modify: affected unit tests

- [ ] Update caller tests to use the caller-owned Repository access, then verify RED.
- [ ] Replace the injection, delete UserQueryService, run the full suite with a local test JWT secret, and commit `refactor: UserQueryService 의존 제거`.
