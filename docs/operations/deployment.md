# 배포와 운영 컨벤션

## 환경

환경은 `local`, `dev`, `prod`로 구분합니다. 환경마다 설정과 접근 권한을 분리합니다.

| 환경 | Spring Profile | 기준 브랜치 | 용도 | 배포 방식 |
| --- | --- | --- | --- | --- |
| `local` | `local` | 작업 브랜치 | 개인 개발과 테스트 | 개발자가 직접 실행 |
| `dev` | `dev` | `develop` | 통합 테스트와 QA | `develop` 병합 후 GitHub Actions가 자동 배포 |
| `prod` | `prod` | `main` | 실제 서비스 운영 | `main` 병합 후 GitHub Actions가 자동 배포 |

## 배포 흐름

### 기능 개발과 `dev` 배포

1. 기능, 개선, 리팩터링 Issue를 생성합니다.
2. `develop`에서 `type/#issueNumber` 형식의 작업 브랜치를 분기합니다.
3. 로컬에서 구현하고 테스트합니다.
4. `develop`을 대상으로 Pull Request(PR)를 생성합니다.
5. CI와 리뷰 게이트를 통과합니다.
6. `develop`에 병합합니다.
7. GitHub Actions가 `dev` 환경에 자동 배포합니다.
8. `dev` 환경에서 E2E 테스트와 QA를 진행합니다.

### 운영 릴리즈와 `prod` 배포

1. `dev` 환경의 E2E 테스트와 QA를 완료합니다.
2. `main`을 대상으로 릴리즈 PR을 생성합니다.
3. CI와 리뷰 게이트를 통과합니다.
4. `main`에 병합합니다.
5. GitHub Actions가 `prod` 환경에 자동 배포합니다.

### QA 중 버그 수정

1. `dev` 환경 QA에서 버그를 발견하면 Bug Issue를 생성합니다.
2. `main`에서 `hotfix/{issue_number}` 브랜치를 분기합니다.
3. 로컬에서 수정하고 테스트합니다.
4. `main`을 대상으로 Hotfix PR을 생성합니다.
5. CI와 리뷰 게이트를 통과합니다.
6. `main`에 병합합니다.
7. GitHub Actions가 `prod` 환경에 자동 배포합니다.

## 환경별 금지 사항

### `local`

- `dev` 또는 `prod` 데이터베이스, Redis, Secret에 직접 연결하지 않습니다.
- `prod` 데이터나 Secret을 로컬 설정 파일, 로그, 테스트 데이터에 복사하지 않습니다.
- 로컬 검증만으로 `develop` 또는 `main`에 직접 push하지 않습니다.

### `dev`

- CI/CD를 우회한 EC2 컨테이너와 설정 변경, 수동 배포를 금지합니다.
- 검증하지 않은 데이터베이스 마이그레이션, Secret, 외부 연동 변경을 적용하지 않습니다.
- 운영 데이터와 운영 Secret을 사용하지 않습니다.

### `prod`

- `main` 이외 브랜치의 코드나 이미지를 배포하지 않습니다.
- EC2 SSH 또는 Docker Compose로 GitHub Actions 배포 절차를 우회하지 않습니다.
- 검증하지 않은 마이그레이션을 실행하지 않습니다.
- Secret이나 개인정보를 코드, 이미지, 로그에 남기지 않습니다.

## 설정과 Secret

환경별 설정은 `application-{profile}.yml` 파일로 분리합니다.

- 실제 값은 환경 변수로 주입합니다.
- 실제 값을 제외한 설정은 Git으로 관리합니다.
- 보안상 노출해도 되는 값은 마스킹하지 않고 설정 파일에 기록합니다.

Secret은 코드, Git, Docker 이미지, 로그에 남기지 않습니다. 배포 환경에는 Secret 관리 체계로 값을 주입합니다. 현재는 GitHub Actions Environment Secrets를 사용합니다.

설정을 변경하면 PR 설명에 다음 내용을 기록합니다.

- 적용 환경
- 영향 범위
- 롤백 방법
