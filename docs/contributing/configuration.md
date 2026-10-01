# Configuration

설정은 적용 환경과 검증 대상을 먼저 확인합니다. 환경과 배포 기준은 [Deployment](../operations/deployment.md)가 소유합니다.

| 환경 | 확인할 파일 | 용도 |
| --- | --- | --- |
| local | `src/main/resources/application-local.yml` | 개인 개발 |
| dev | `src/main/resources/application-dev.yml` | 통합 및 QA |
| prod | `src/main/resources/application-prod.yml` | 운영 |
| test | `src/test/resources/application-test.yml` | 통합 테스트 |

설정 항목을 추가하거나 삭제하면 local과 test를 함께 확인합니다. Secret은 [Deployment](../operations/deployment.md#설정과-secret)를 따르며 테스트 데이터에도 넣지 않습니다.

PR에는 적용 환경, 영향 범위와 rollback 또는 후속 수정 방법을 적습니다. migration이 포함되면 [Persistence](../architecture/persistence.md)의 백업과 복구 기준을 확인합니다.
