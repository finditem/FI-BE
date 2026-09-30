# Git 컨벤션

## 목적

Issue, 브랜치, 커밋, Pull Request(PR)에서 작업 성격을 같은 type으로 표시합니다. 팀원은 변경 범위와 병합 대상을 빠르게 확인할 수 있습니다.

## 작업 성격

Issue 제목 prefix, 작업 성격 라벨, 브랜치 type, PR 제목 type, 커밋 type에는 같은 값을 사용합니다.

Issue와 PR에는 작업 성격 라벨을 1개 적용합니다.

| 성격 | Issue 제목 | 작업 성격 라벨 | 브랜치 | PR·커밋 |
| --- | --- | --- | --- | --- |
| 기능 | `[FEAT] 제목` | `:sparkles: feature` | `feat/#번호` | `feat: 제목` |
| 버그 수정 | `[FIX] 제목` | `:bug: fix` | `fix/#번호` | `fix: 제목` |
| 문서 | `[DOCS] 제목` | `:memo: docs` | `docs/#번호` | `docs: 제목` |
| 코드 스타일 | `[STYLE] 제목` | `:art: style` | `style/#번호` | `style: 제목` |
| 리팩터링 | `[REFACTOR] 제목` | `:recycle: refactor` | `refactor/#번호` | `refactor: 제목` |
| 테스트 | `[TEST] 제목` | `:white_check_mark: test` | `test/#번호` | `test: 제목` |
| 설정·빌드 | `[CHORE] 제목` | `:building_construction: chore` | `chore/#번호` | `chore: 제목` |
| 이름·위치 변경 | `[RENAME] 제목` | `:truck: rename` | `rename/#번호` | `rename: 제목` |
| 성능 | `[PERF] 제목` | `:zap: perf` | `perf/#번호` | `perf: 제목` |

- `style`은 코드 포맷, 공백, 줄바꿈, import처럼 기능에 영향을 주지 않는 변경에 사용합니다.
- `rename`은 파일, 패키지, 클래스, 메서드 이름을 바꾸거나 위치를 옮길 때 사용합니다.
- `bug`, `documentation`, `enhancement`, `question`, `D-*`는 작업 성격 라벨과 함께 붙일 수 있는 보조 라벨입니다.
- 버그를 제보할 때는 `[BUG] 제목`과 `bug` 라벨을 사용합니다. 버그를 고칠 때는 `[FIX] 제목`과 `:bug: fix` 라벨을 사용합니다.
- `:rocket: release`는 현재 커밋 type과 Commitlint 허용 type에 없으므로 사용하지 않습니다.

## 브랜치

### 생성과 이름

- 모든 작업은 Issue를 만든 뒤 브랜치를 생성합니다.
- `main`과 `develop`에 직접 push하지 않습니다.
- Issue를 PR 하나로 완료할 때는 구현 브랜치 이름으로 `type/#issueNumber` 형식을 사용합니다.
- 하나의 Issue를 두 개 이상 PR로 나눌 때는 `type/#issueNumber-subNumber` 형식을 사용합니다. `subNumber`는 첫 PR부터 `1`씩 늘립니다.

```text
feat/#3
refactor/#29
feat/#3-1
feat/#3-2
```

- 의존하지 않는 PR은 `develop`을 대상으로 합니다.
- 의존 PR은 GitHub Stacked PR로 만듭니다.
- 병합한 브랜치는 해당 Issue의 후속 PR이 없을 때 삭제합니다.

### 변경 범위

성격이 다른 변경은 가능한 한 별도 PR로 만듭니다. 기능, 리팩터링, 대규모 포맷 변경, 의존성 갱신은 각각 분리합니다.

### 의존 브랜치와 GitHub Stacked PR

의존 PR은 [GitHub Stacked PR](https://docs.github.com/en/pull-requests/get-started/about-stacked-prs)로 만듭니다. 첫 PR은 `develop`을 대상으로 합니다. 다음 PR은 바로 아래 PR의 브랜치를 대상으로 합니다.

```text
develop ← refactor/#527-1 ← refactor/#527-2 ← refactor/#527-3
```

`refactor/#527-2`의 PR 대상은 `refactor/#527-1`입니다. `refactor/#527-3`의 PR 대상은 `refactor/#527-2`입니다. `gh stack` 확장을 사용하거나 GitHub에서 PR을 만들 때 바로 아래 브랜치를 대상으로 선택합니다.

### Worktree

Worktree를 사용할 때는 선택적으로 저장소 루트의 `.worktrees/{type}-{issue-number}`에 생성합니다.

## 커밋

- 하나의 의도가 드러나는 단위로 커밋합니다.
- 커밋 메시지는 `type: 변경 내용 요약` 형식을 사용합니다.
- `type(scope): summary` 형식은 사용하지 않습니다.
- 커밋 메시지는 한글 개조식으로 작성합니다.
- 변경 방법이 아니라 변경 대상을 작성합니다.

```text
feat: 로그인 기능 구현
```

허용 type과 type별 기준은 [작업 성격](#작업-성격)을 따릅니다.

### 브랜치 보호 규칙

[Deployment](../operations/deployment.md)의 서버 환경 분리를 바탕으로 보호 규칙을 만듭니다.

#### develop

개발 서버 브랜치 규칙입니다.

- Restrict deletions(브랜치 삭제 금지)
- Require linear history(머지 커밋 금지, 히스토리 깔끔하게 유지. Squash/Rebase만 허용)
- Require a pull request before merging(PR을 통해서만 머지 가능)
    - Required approvals: 0(0명 이상 승인 필요)
    - Dismiss stale pull request approvals when new commits are pushed: ON(새 커밋이 올라오면 기존 승인 무효)
    - Require conversation resolution before merging: ON(리뷰 코멘트 전부 Resolve 되어야 머지 가능)
    - Require an additional approval for unattributed Copilot pull requests: ON(Copilot이 혼자 만든 PR은 사람이 만든 PR보다 승인을 1명 더 받아야 한다)
    - Allowed merge methods(Squash 허용)
- Require status checks to pass
    - review-gate
- Block force pushes(강제 푸시 방지)

#### main

운영 서버 브랜치 규칙입니다.

- Restrict deletions(브랜치 삭제 금지)
- Require a pull request before merging(PR을 통해서만 머지 가능)
    - Required approvals: 0(0명 이상 승인 필요)
    - Require an additional approval for unattributed Copilot pull requests: ON(Copilot이 혼자 만든 PR은 사람이 만든 PR보다 승인을 1명 더 받아야 한다)
- Allowed merge methods(Merge commit / Squash / Rebase 허용)
- Block force pushes(강제 푸시 방지)

## Issue

### 작업 Issue

Issue에 작업 이유, 범위, 완료 기준을 적습니다. 하나의 Issue는 여러 PR로 나눌 수 있습니다. 병렬로 작업하거나 따로 완료할 수 있을 때만 하위 Issue를 만듭니다.

```text
제목: [CHORE] 작업 제목
라벨: :building_construction: chore

> 작업 성격에 따라 [작업 성격](#작업-성격)의 제목 prefix와 라벨을 선택한다.

## 💡 작업 내용

- 이번 작업의 개요를 작성한다.

## ✅ 상세 내용

구현 방향은 작업 중 바뀔 수 있습니다. 상세 내용은 미리 적지 않아도 됩니다.

## 📢 참고 사항

- 리뷰어와 팀원이 알아야 할 추가 맥락을 작성한다.

## 🎯 기대 결과

- 사용자가 확인할 수 있는 최종 결과 또는 데모 기준을 작성한다.

## ⚖️ 브랜치/PR 정책 확인

- [ ] develop에서 브랜치를 생성한다. (예외적으로 의존 브랜치에서 분기 가능)
- [ ] main, develop에 직접 push/commit 하지 않는다 (반드시 PR로 머지).
- [ ] PR 대상은 develop 브랜치다.
- [ ] 커밋 메시지는 Conventional Commits 양식을 따른다 (feat/fix/chore/docs/refactor/test 등).
```

작업 성격에 맞는 Issue 제목 prefix와 라벨은 [작업 성격](#작업-성격)에서 선택합니다.

상위 Issue에는 작업 배경, 목표, 완료 기준을 적습니다. 하위 Issue에는 독립적으로 끝낼 작업을 적고 상위 Issue 번호를 참조합니다.

### 병합 후 오류

PR을 병합한 뒤 오류가 발생하면, 기존 Issue의 완료 조건을 기준으로 처리합니다.

- 기존 Issue의 완료 조건을 충족하지 못했으면 기존 Issue를 재오픈합니다. 후속 PR은 오류 수정이므로 `fix` type을 사용합니다.
- 기존 Issue의 완료 조건과 직접 관련 없는 오류가 발생했으면 새 `[BUG]` Issue를 만듭니다. 새 `[FIX]` Issue와 PR로 수정합니다.
- `develop`에 병합한 뒤에만 확인할 수 있는 배포, 외부 연동, 통합 환경 검증은 기존 Issue의 기대 결과에 완료 조건으로 작성합니다.
- 재오픈한 Issue 본문은 수정하지 않습니다. 오류 내용, 발생 환경, 후속 PR 번호는 Comment에 기록합니다.

```text
#10 [FEAT] 배포 스크립트에 Slack 알림 추가
└─ PR #20 feat: Slack 알림 추가 → develop 병합 후 배포 오류 발생

#10 재오픈
Comment: develop 배포 실패, 후속 PR #21
└─ PR #21 fix: Slack 알림 전송 조건 수정 → develop 배포 성공
```

`#10`은 Slack 알림 추가가 완료되지 않아 재오픈합니다. `#21`은 오류를 고치므로 `fix` type을 사용합니다.

### 버그 제보 Issue

```text
제목: [BUG] 버그 제목
라벨: bug

## 버그 설명

- 발생한 버그에 대해 간단히 설명해주세요.

## 재현 방법

1. ...
2. ...
3. ...

## 기대 동작

- 정상적으로 동작해야 하는 내용을 작성해주세요.

## 실제 동작

- 실제로 어떻게 동작했는지 작성해주세요.

## 환경

- OS/브라우저/앱 버전 등 (예: Windows 11, Chorme 128)

## 스크린샷

- 필요하다면 스크린샷/로그를 첨부해주세요.
```

## Pull Request

### PR 템플릿

```text
## 🧩 관련 이슈

- `related #이슈번호` (중간 PR)
- `close #이슈번호` (Issue를 끝내는 PR)
- 상위 Issue: #번호 (하위 Issue인 경우에만 작성)

## 🛠️ 작업 내용

-
-

## 📢 참고 및 특이사항

-

## ✅ 체크리스트

- [ ] Merge 대상 브랜치가 **develop** 인지 확인
- [ ] PR 제목 형식 준수 (예: `feat: 로그인 기능 구현`)
- [ ] 관련 이슈 연결 (`close #이슈번호`)
- [ ] 불필요한 코드/주석 제거
- [ ] 기능 정상 작동 확인
```

### 병합

- 중간 PR에는 `related #이슈번호`를 적습니다.
- Issue를 끝내는 PR에만 `close #이슈번호`를 적습니다.
- 각 PR은 리뷰하고 검증할 수 있는 범위로 나눕니다.

- Squash and merge 방식으로 병합합니다.
    - title에 `{PR 제목} (#{PR번호})`를 적습니다.
    - body에는 기본적으로 적지 않습니다.
- 최소 1명의 승인을 받은 뒤 병합합니다.
    - 브랜치 룰에 0명으로 설정한 이유는 review-gate의 구현 한계입니다.
- 모든 리뷰 대화를 해결한 뒤 병합합니다. AI 리뷰도 포함합니다.
- 리뷰 뒤 변경 사항을 추가하면 다시 검토받습니다.

## 핫픽스

`dev` 환경 QA 중 발견한 버그는 [Deployment의 QA 중 버그 수정](../operations/deployment.md#qa-중-버그-수정) 절차를 따릅니다.

1. Bug Issue를 생성하고 `main`에서 `hotfix/{issue_number}` 브랜치를 분기합니다.
2. 로컬에서 수정하고 테스트한 뒤 `main` 대상 Hotfix PR을 만듭니다.
3. CI와 리뷰 게이트를 통과해 `main`에 병합하면 GitHub Actions가 `prod`에 배포합니다.

긴급 배포에서 승인 예외를 적용하는 별도 절차는 아직 정하지 않았습니다. 예외가 필요하면 팀과 논의합니다.
