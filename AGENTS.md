# FindItem Backend Agent Guide

이 저장소는 FindItem 백엔드의 구현과 개발 문서를 함께 관리한다. 사람과 AI가 같은 근거를 찾을 수 있도록, 이 파일은 작업별 문서 경로와 실행 절차만 안내한다. 확정된 규칙의 원본은 연결된 `docs/` 문서에 둔다.

## Repository Structure

```text
FI-BE/
├── src/
│   ├── main/
│   │   ├── java/com/fmi/
│   │   │   ├── domain/      # 업무 기능, 상태와 데이터 접근
│   │   │   ├── global/      # 여러 도메인의 공통 기술 코드
│   │   │   └── external/    # 외부 기술 연동
│   │   └── resources/db/migration/ # Flyway schema
│   └── test/                # 단위 테스트와 통합 테스트
├── docs/                    # 아래 Documentation Map 참고
└── AGENTS.md
```

현재 코드에는 이 기본 구조와 다른 과거 배치도 있다. 문서 형식을 맞추기 위해 관련 없는 코드를 이동하지 않는다.

## Documentation Map

먼저 [Architecture Overview](docs/architecture/overview.md)를 읽고 작업에 맞는 원본 문서를 선택한다. 전체 문서 목록은 [Documentation Map](docs/README.md)에 있다.

| 작업 | 읽을 문서 |
| --- | --- |
| 구조 설계와 코드 리뷰 | [Decision Guide](docs/architecture/decision-guide.md), [Domain Boundaries](docs/architecture/domain-boundaries.md) |
| DB와 migration | [Persistence](docs/architecture/persistence.md), [Testing](docs/contributing/testing.md) |
| 기능 추가 | [Adding Feature](docs/contributing/adding-feature.md), [Error Handling](docs/contributing/error-handling.md) |
| 버그 수정 | [Fixing Bugs](docs/contributing/fixing-bugs.md), [Testing](docs/contributing/testing.md) |
| 리팩터링 | [Refactoring](docs/contributing/refactoring.md), [Code Style](docs/contributing/code-style.md) |
| Java 코드 또는 설정 | [Contributing Overview](docs/contributing/overview.md), [Code Style](docs/contributing/code-style.md), 필요하면 [Configuration](docs/contributing/configuration.md) |
| 배포와 운영 | [Operations Overview](docs/operations/overview.md), [Deployment](docs/operations/deployment.md) |
| 문서 변경 | [Documentation](docs/contributing/documentation.md) |
| Issue, 브랜치, 커밋, PR | [Git Guide](docs/contributing/git.md) |

업무 정책은 [Policies Overview](docs/policies/overview.md)에 적힌 소유 원칙을 확인하고 DOCS의 관련 확정 정책서를 읽는다. FI-BE 코드는 현재 동작의 증거이며 정책을 확정하는 근거는 아니다.

## 문서에 없는 정책

문서에는 사람이 알고 있는 모든 요구와 결정이 담겨 있지 않다. 결과, 권한, 데이터 보관, 동시성 또는 외부 효과가 달라지는 선택을 기존 코드나 일반 관례만으로 확정하지 않는다.

1. 확인한 사실, 문서에 없는 조건, 가능한 선택과 각 결과를 구분한다.
2. 필요한 질문을 구현 전에 묶어서 묻고, 답과 무관한 코드 조사는 계속한다.
3. 답을 기다리는 동안 임의 정책을 코드나 테스트에 고정하지 않는다.
4. 구현 선택만 남았고 기존 원칙으로 판단할 수 있다면 직접 결정하고 근거를 적는다.
5. 결정이 확정되면 원본 문서의 소유 위치를 확인하고 코드와 테스트를 함께 갱신한다.

기존 문서와 구현이 다르면 차이를 드러낸다. 팀 논의가 필요한 항목은 해당 문서의 기준을 따르며, AI가 혼자 정책을 확정하지 않는다.

## Workflow

1. Issue와 관련 문서, 실제 코드와 테스트를 읽는다.
2. 입력과 출력, read와 write, transaction, 외부 호출과 실패 뒤 상태를 적는다.
3. [Decision Guide](docs/architecture/decision-guide.md)로 코드 위치와 DB 접근 방식을 정한다.
4. 주변 구현을 참고하되 필요 없는 class, interface와 공통 추상화를 추가하지 않는다.
5. 위험한 가정을 [Testing](docs/contributing/testing.md)에 맞춰 검증한다.
6. 코드와 원본 문서의 변경 사항을 함께 확인한다.

## Change Boundaries

- 새 도메인 분리, `global` 배치, `service/internal`의 도입 시점, port 또는 직접 의존은 [Domain Boundaries](docs/architecture/domain-boundaries.md)의 팀 논의 조건을 따른다.
- JPA 관계, QueryDSL, native SQL과 Flyway 변경은 [Persistence](docs/architecture/persistence.md)를 따른다.
- 외부 서비스, 배포, Secret 또는 데이터 삭제처럼 실제 환경에 영향을 주는 작업은 담당자에게 확인한다.
- 문서 형식을 맞추기 위한 대규모 코드 이동이나 새 업무 정책 추가는 하지 않는다.
- generated code를 직접 수정하지 않는다.

## Commands

Gradle은 Java 17로 실행한다. SDKMAN을 사용할 수 있으면 설치된 Java 17 후보를 `sdk use java <Java 17 후보>`로 선택한다. 저장소에 `.sdkmanrc`가 있다면 `sdk env`를 사용한다. SDKMAN이 없으면 설치된 Java 17 JDK를 `JAVA_HOME`으로 선택한다. `java -version`으로 17인지 확인한다. 적합한 JDK가 없으면 경로 또는 설치 방법을 사용자에게 확인한다. Gradle toolchain 설정만으로 Gradle 실행 JVM의 버전이 바뀌지는 않는다.

```bash
./gradlew spotlessApply  # 커밋 전 항상 실행하고 적용된 변경을 검토
./gradlew test           # 변경과 관련된 테스트 검증
./gradlew spotlessCheck  # CI 또는 별도 형식 검사가 필요할 때 실행
```

커밋 전에는 변경 파일 종류와 관계없이 `spotlessApply`를 실행한다. Gradle 실행이 실패하면 cache를 삭제하지 말고 실패 종류를 확인한다.

- 기존 cache의 lock이나 접근 권한 문제라면 해당 경로의 실행 권한을 요청한 뒤 같은 명령을 다시 실행한다.
- 제한된 환경에서 별도의 쓰기 가능 cache가 필요하면 `GRADLE_USER_HOME`을 지정한다.
- 임시 cache의 `metadata.bin`이 없거나 읽히지 않으면 기존 cache를 건드리지 않고 새로운 임시 경로를 지정해 재시도한다. 예: `GRADLE_USER_HOME="$(mktemp -d)" ./gradlew --no-daemon spotlessApply`.
- 재시도에도 실패하면 첫 유의미한 오류와 cache 경로를 보고한다.

Issue, 브랜치, PR과 scope 없는 `type: 한글 요약` 커밋 형식은 [Git Guide](docs/contributing/git.md)를 따른다. 사용자가 커밋 전 보고를 요청했다면 검증 결과와 변경 목록을 먼저 보고하고, 답을 받은 뒤 커밋한다.

## Review Output

코드와 설계를 검토할 때 시작점에서 결과까지의 흐름, 읽고 쓰는 데이터와 owner, transaction, 외부 호출 순서, 최종 정합성 방어선, 실패와 재시도, 검증 방법을 구체적으로 설명한다. 근거 없는 정책은 확정된 것처럼 쓰지 않는다.
