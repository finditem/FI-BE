# Contributing Overview

[Architecture Overview](../architecture/overview.md)와 관련 업무 정책을 확인한 뒤 작업합니다. 아래 문서는 기존 컨벤션을 실제 변경에 적용하는 순서입니다.

| 작업 | 문서 | 함께 읽을 기준 |
| --- | --- | --- |
| 기능 추가 | [Adding Feature](adding-feature.md) | [Decision Guide](../architecture/decision-guide.md), [Persistence](../architecture/persistence.md) |
| API 설계와 Swagger | [API Pattern](api-pattern.md), [OpenAPI Guide](openapi-guide.md) | DOCS의 API 스펙과 공통 API 스펙 |
| 버그 수정 | [Fixing Bugs](fixing-bugs.md) | [Testing](testing.md), [Error Handling](error-handling.md) |
| 리팩터링 | [Refactoring](refactoring.md) | [Domain Boundaries](../architecture/domain-boundaries.md), [Code Style](code-style.md) |
| Java와 테스트 | [Code Style](code-style.md), [Testing](testing.md) | 변경된 경계의 기존 코드 |
| 문서 | [Documentation](documentation.md) | 변경 대상의 원본 문서 |
| 설정과 운영 | [Configuration](configuration.md), [Operations Overview](../operations/overview.md) | 적용 환경과 rollback |
| 커밋과 PR | [Git Guide](git.md) | Issue와 PR 템플릿 |

## 변경 순서

1. 요청과 확인되지 않은 업무 정책을 구분합니다.
2. 코드의 시작점부터 DB 쓰기와 외부 효과까지 따라갑니다.
3. 변경을 구현하고 가장 위험한 가정을 테스트합니다.
4. 원본 문서와 코드가 달라졌다면 관련 문서를 갱신합니다.
5. 커밋 전에는 변경 파일 종류와 관계없이 `./gradlew spotlessApply`를 실행하고 적용 결과를 검토합니다.
6. [Git Guide](git.md)에 따라 Issue, 브랜치, 커밋과 PR 형식을 확인합니다.

미확정 정책은 [AGENTS.md](../../AGENTS.md#문서에-없는-정책)를 따릅니다.
