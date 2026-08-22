# auth·user 도메인 리팩터링 handoff

## 작업 위치

- Repository: `FI-BE`
- Worktree: `FI-BE/.worktrees/refactor-535`
- Branch: `refactor/#535`
- Base: `refactor/#528`
- Issue: #535
- 원격 push, PR 생성, develop·main 및 dev·prod 설정 변경은 하지 않았다.

## 완료한 커밋

1. `096fdb7 docs: auth user 도메인 리팩터링 계획 추가`
   - 설계와 구현 계획을 추가했다.
2. `6e586da9 refactor: User 비밀번호 상태 전이 캡슐화`
   - 임시 비밀번호 발급·복원·일반 변경을 `User` 행위로 옮겼다.
3. 현재 커밋
   - User 계정 상태 전이와 관련 Service 위임을 담는다.

## 현재 커밋의 내용

- `User`에 다음 상태 전이를 추가했다.
  - 약관 동의, 이메일 인증 완료
  - 닉네임·프로필 이미지 변경과 프로필 수정 시각 기록
  - 탈퇴 사유 기록과 탈퇴 처리
  - 소셜 로그인 재가입 시 탈퇴·동의·탈퇴 사유 초기화
- `EmailVerificationService`, `SocialLoginService`, `WithdrawalService`, `UserService`가 해당 행위를 호출하도록 바꿨다.
- Service는 사용자 조회, 입력 검증, S3·OAuth·메일·토큰·게시글 처리의 순서를 계속 가진다.
- `UserTest`는 DCI(Describe–Context–It) 구조를 기본으로 하고 모든 테스트에 `// given`, `// when`, `// then`을 사용한다.
- 빈 프로필 수정 요청도 이전과 동일하게 `updatedAt`을 갱신한다.

## 검증

다음 명령이 local profile 및 worktree에서 통과했다.

```bash
GRADLE_USER_HOME=/private/tmp/finditem-gradle ./gradlew --no-daemon test \
  --tests com.fmi.domain.user.data.UserTest \
  --tests com.fmi.domain.auth.service.WithdrawalServiceTest \
  --tests com.fmi.domain.auth.service.SocialLoginServiceTest \
  --tests com.fmi.domain.auth.service.EmailVerificationServiceTest
```

- `.agent-pilot/agent-guard format`도 통과했다.
- 전체 테스트는 시작 시 `JWT_SECRET` 부재로 실패한다. local 전용 더미 `JWT_SECRET`을 주면 기준 브랜치에서 전체 테스트 통과를 확인했다. 다음 전체 검증에도 local 환경 변수만 사용한다.

## 다음 작업 순서

1. `PasswordResetService`, `TemporaryPasswordCleanupScheduler`, `PasswordService`의 비밀번호 setter 흐름을 이미 추가한 `User` 행위로 교체한다.
2. 반복이 확인되는 경우에만 `auth/service/internal/PasswordPolicy`, `PasswordCodec`을 도입한다. `PasswordEncoder`는 일반 import로 사용한다.
3. `AccountAuthController`의 기존 경로를 유지한 채 `AuthController`에 통합한다.
4. `UserQueryService`를 호출자 소유 Service·Repository 접근으로 치환한 뒤 삭제한다.
5. 남은 `User` setter 호출을 다시 전수 조사한 뒤에만 Lombok `@Setter` 제거를 판단한다. auth·user 범위 밖 호출자는 기계적으로 바꾸지 않는다.

## 작업 규칙

- 새 production 행위는 RED–GREEN 단위 테스트를 먼저 작성한다.
- 테스트 이름은 번역체를 피하고, 상태 분기가 있을 때만 Context depth를 추가한다.
- `QueryService` 계층을 새로 만들지 않는다.
- commit 전에는 변경 범위·테스트·정확한 메시지를 사용자에게 보고하고 승인받는다.
- commit 전 `commit-port.md`를 읽고 `agent-guard commit-draft`를 실행하며, commit 후 `agent-guard verify`를 실행한다.
