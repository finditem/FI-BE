# Documentation Map

FI-BE의 백엔드 개발 문서는 이 저장소에서 관리한다. 작업을 시작할 때 [AGENTS.md](../AGENTS.md)의 작업별 경로를 먼저 확인한다.

| 영역 | 시작 문서 | 다루는 내용 |
| --- | --- | --- |
| 설계 | [Architecture Overview](architecture/overview.md) | 실행 흐름, 도메인 경계, 데이터 소유자 |
| 설계 판단 | [Decision Guide](architecture/decision-guide.md) | 구조, 영속성, 외부 연동 선택 기준 |
| 개발 | [Contributing Overview](contributing/overview.md) | 기능 추가, 버그 수정, 리팩터링과 검증 |
| 운영 | [Operations Overview](operations/overview.md) | 배포 환경, 설정과 모니터링 |
| 업무 정책 | [Policies Overview](policies/overview.md) | 정책 원본의 위치와 확인 방법 |

옮겨온 문서는 기존 백엔드 컨벤션의 의미를 유지한다. 새로 작성한 작업 가이드는 그 컨벤션을 실제 코드에 적용하는 순서를 설명한다. 같은 규칙을 여러 문서에 중복해서 정의하지 않는다.

코드는 현재 동작의 근거다. 코드와 확정된 문서가 다르면 차이를 드러내고 팀에서 결정한 뒤 구현과 문서를 함께 고친다.
