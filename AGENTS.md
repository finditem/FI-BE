# FindItem Backend Agent Guide

작업별 문서와 실행 절차를 안내합니다. 규칙의 원본은 `docs/`에 둡니다.

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

## 레거시 코드와 리팩터링

- 기존 레거시 기능을 수정하거나 추가할 때는 해당 영역의 명명, 구조와 호출 스타일을 유지합니다. 부분적인 컨벤션 정리와 리팩터링을 섞지 않습니다.
- 새 도메인과 새 Use Case는 프로젝트 가이드를 따릅니다. 기존 레거시 도메인을 호출하는 접점은 해당 코드의 방식을 유지합니다.
- 리팩터링은 [Refactoring](docs/contributing/refactoring.md)의 E2E 회귀 테스트, 흐름 파악, 미확정 사항 합의, 메시지와 경계 정리, Use Case 전체 재작성 순서를 따릅니다.
- 운영 위험이 큰 경우에만 Feature Flag로 Legacy와 New 경로를 점진 전환합니다.
- 기능 작업에서 발견한 리팩터링 후보는 별도 작업으로 제안합니다.

## Documentation Map

먼저 [Architecture Overview](docs/architecture/overview.md)를 읽고 작업에 맞는 원본 문서를 선택합니다. 전체 문서 목록은 [Documentation Map](docs/README.md)에 있습니다.

| 작업 | 읽을 문서 |
| --- | --- |
| 구조 설계와 코드 리뷰 | [Decision Guide](docs/architecture/decision-guide.md), [Domain Boundaries](docs/architecture/domain-boundaries.md) |
| DB와 migration | [Persistence](docs/architecture/persistence.md), [Testing](docs/contributing/testing.md) |
| 기능 추가 | [Adding Feature](docs/contributing/adding-feature.md), [Error Handling](docs/contributing/error-handling.md) |
| API 설계와 Swagger | [API Pattern](docs/contributing/api-pattern.md), [OpenAPI Guide](docs/contributing/openapi-guide.md) |
| 버그 수정 | [Fixing Bugs](docs/contributing/fixing-bugs.md), [Testing](docs/contributing/testing.md) |
| 리팩터링 | [Refactoring](docs/contributing/refactoring.md), [Code Style](docs/contributing/code-style.md) |
| Java 코드 또는 설정 | [Contributing Overview](docs/contributing/overview.md), [Code Style](docs/contributing/code-style.md), 필요하면 [Configuration](docs/contributing/configuration.md) |
| 배포와 운영 | [Operations Overview](docs/operations/overview.md), [Deployment](docs/operations/deployment.md) |
| 문서 변경 | [Documentation](docs/contributing/documentation.md) |
| Issue, 브랜치, 커밋, PR | [Git Guide](docs/contributing/git.md) |

업무 정책은 [Policies Overview](docs/policies/overview.md)에 적힌 소유 원칙을 확인하고 DOCS의 관련 확정 정책서를 읽습니다. FI-BE 코드는 현재 동작의 증거이며 정책을 확정하는 근거는 아닙니다.

## 문서에 없는 정책

사용자 결과, 권한, 데이터 보관, 동시성, 외부 부수 효과가 달라지는 미확정 정책은 기존 코드나 관례로 결정하지 않습니다.

1. 확인한 사실, 미확정 조건, 선택지와 결과를 정리합니다.
2. 구현 전에 질문을 묶어 묻고, 답과 무관한 조사는 계속합니다.
3. 답을 기다리는 동안 임의 정책을 코드나 테스트에 고정하지 않습니다.
4. 기존 원칙으로 판단할 수 있는 구현 선택은 직접 결정하고 근거를 적습니다.
5. 확정된 결정은 소유 문서, 코드와 테스트에 반영합니다.

문서와 구현의 차이는 보고하고, 팀 논의는 해당 문서의 기준을 따릅니다.

## Workflow

1. Issue와 관련 문서, 실제 코드와 테스트를 읽습니다.
2. 입력과 출력, read와 write, transaction, 외부 호출과 실패 뒤 상태를 적습니다.
3. [Decision Guide](docs/architecture/decision-guide.md)로 코드 위치와 DB 접근 방식을 정합니다.
4. [레거시와 리팩터링 기준](#레거시-코드와-리팩터링)에 따라 적용할 스타일을 정합니다. 불필요한 class, interface와 공통 추상화는 추가하지 않습니다.
5. 위험한 가정을 [Testing](docs/contributing/testing.md)에 맞춰 검증합니다.
6. 코드와 원본 문서의 변경 사항을 함께 확인합니다.

## Change Boundaries

- 새 도메인 분리, `global` 배치, `service/internal`의 도입 시점, port 또는 직접 의존은 [Domain Boundaries](docs/architecture/domain-boundaries.md)의 팀 논의 조건을 따릅니다.
- JPA 관계, QueryDSL, native SQL과 Flyway 변경은 [Persistence](docs/architecture/persistence.md)를 따릅니다.
- 외부 서비스, 배포, Secret 또는 데이터 삭제처럼 실제 환경에 영향을 주는 작업은 담당자에게 확인합니다.
- 새 업무 정책은 팀 합의 후 반영합니다.
- generated code를 직접 수정하지 않습니다.

## Commands

Gradle 실행 JVM은 Java 17을 사용합니다. toolchain 설정과 별도로 `java -version`을 확인합니다.

| 환경 | JDK 선택 |
| --- | --- |
| SDKMAN과 `.sdkmanrc` 사용 | `sdk env` |
| SDKMAN 사용 | `sdk use java <Java 17 후보>` |
| SDKMAN 미사용 | 설치된 Java 17의 `JAVA_HOME` 설정 |
| Java 17 미설치 | 사용자에게 경로 또는 설치 방법 확인 |

```bash
./gradlew spotlessApply  # 파일 종류와 관계없이 커밋 전 실행, 적용 결과 검토
./gradlew test           # 전체 테스트 검증
./gradlew spotlessCheck  # CI 또는 별도 형식 검사가 필요할 때 실행
```

Gradle 실패 시 cache를 삭제하지 않고 원인을 확인합니다.

- 기존 cache의 lock이나 접근 권한 문제라면 해당 경로의 실행 권한을 요청한 뒤 같은 명령을 다시 실행합니다.
- 제한된 환경에서 별도의 쓰기 가능 cache가 필요하면 `GRADLE_USER_HOME`을 지정합니다.
- 임시 cache의 `metadata.bin`이 없거나 읽히지 않으면 기존 cache를 건드리지 않고 새로운 임시 경로를 지정해 재시도합니다. 예: `GRADLE_USER_HOME="$(mktemp -d)" ./gradlew --no-daemon spotlessApply`.
- 재시도에도 실패하면 첫 유의미한 오류와 cache 경로를 보고합니다.

Issue, 브랜치, PR과 scope 없는 `type: 한글 요약` 커밋 형식은 [Git Guide](docs/contributing/git.md)를 따릅니다. 사용자가 커밋 전 보고를 요청했다면 검증 결과와 변경 목록을 먼저 보고하고, 답을 받은 뒤 커밋합니다.

## Review Output

코드와 설계를 검토할 때 시작점에서 결과까지의 흐름, 읽고 쓰는 데이터와 owner, transaction, 외부 호출 순서, 최종 정합성 방어선, 실패와 재시도, 검증 방법을 구체적으로 설명합니다. 근거 없는 정책은 확정된 것처럼 쓰지 않습니다.
