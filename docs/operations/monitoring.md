# Monitoring

기술 구성은 [Stack](../architecture/stack.md), 실제 컨테이너 설정은 [모니터링 Docker Compose](../../deploy/monitoring/docker-compose.yml)에서 관리합니다.

## 수집과 접근 경로

- Grafana Alloy는 애플리케이션 서버의 로그를 모니터링 서버의 Loki로 전송합니다.
- Prometheus는 애플리케이션 서버의 내부 메트릭 수집 경로에 접근합니다.
- Grafana와 Prometheus의 호스트 포트는 localhost에 바인딩합니다. 운영자는 SSM 포트 포워딩으로 접근합니다.
- Loki는 애플리케이션 서버의 로그 전송을 받아야 하므로 localhost로 제한하지 않습니다. Security Group에서 로그 전송 서버만 허용합니다.

서버 셸과 모니터링 도구의 접속 절차는 팀 Confluence 운영 문서에서 관리합니다. 공개 개발 문서에 운영 접근 주소와 네트워크 세부 사항을 복사하지 않습니다.

## 설정 변경

Grafana 포트 바인딩이나 provisioning 파일을 변경할 때 데이터 볼륨을 유지하고 Grafana 컨테이너만 재생성합니다. 볼륨을 삭제하는 명령을 사용하지 않습니다. 변경 후 SSM 터널의 Grafana 접속과 상태 API, Prometheus 수집 대상, Loki 로그 유입을 확인합니다. provisioning 파일을 변경했다면 알림 규칙이 provisioning으로 표시되는지와 연락처의 테스트 알림도 확인합니다. 실패하면 이전 Compose 설정과 provisioning 파일로 Grafana 컨테이너를 다시 생성합니다.

애플리케이션 서버의 SSH, CI/CD와 Nginx 접근 정책은 이 모니터링 접속 설정과 별도로 관리합니다.

## 애플리케이션 진단 경로

공개 dev/prod 도메인에서 Swagger UI, OpenAPI 명세와 Actuator를 차단합니다. 최소 상태 확인용 `/health`는 유지하며, Docker 헬스체크는 컨테이너 내부의 `/actuator/health`를 사용합니다. Prometheus는 FI-INFRA의 [내부 수집 설정](https://github.com/finditem/FI-INFRA/blob/main/server/nginx/monitoring.conf)을 사용하므로 공개 도메인 제한과 별도로 유지합니다.

Swagger와 Actuator의 운영 접속 절차는 팀 Confluence 운영 문서에서 관리합니다. Loki 네트워크 설정과 애플리케이션 SSH 배포 경로는 변경하지 않습니다.

Actuator health에는 애플리케이션이 동작하는 데 필요한 DB, Redis와 디스크만 포함합니다. 메일 같은 외부 서비스는 health에서 제외하고 [알림 기준](#알림-기준)에 따라 실제 호출 결과로 판단합니다.

## 알림 기준

- 알림은 사용자가 겪는 증상으로 설정합니다. 업무 API 실패, 응답 지연과 기능 실패가 해당합니다.
- 원인 지표는 증상이 나타나기 전에 대응해야 하는 디스크, JVM 힙과 DB 커넥션 대기에만 알림을 설정합니다. 나머지 원인 지표는 대시보드에서 확인합니다.
- 외부 서비스의 동기 호출 실패는 업무 API의 5xx 응답으로, 비동기 호출 실패는 ERROR 로그로 판단합니다.
- CRITICAL 규칙에는 최소 건수와 지속 시간을 함께 조건으로 둡니다. 한 건 발생만으로 CRITICAL 알림을 보내지 않습니다.
- 알림 대상은 prod입니다. 모든 알림은 `slack-monitoring`으로 보내고, 제목의 등급으로 구분합니다.

### 등급

| 등급 | 기준 | 대응 |
| --- | --- | --- |
| CRITICAL | 대부분의 사용자가 핵심 흐름을 사용할 수 없거나, 데이터 손실과 정합성 위험이 있습니다. 방치하면 이 상태가 되는 원인 지표도 포함합니다. | 즉시 확인 |
| WARNING | 서비스는 동작하지만 일부 기능이 실패하거나 느립니다. 부가 기능과 비동기 처리의 실패를 포함합니다. | 업무 시간 안에 확인 |

### 상황별 등급

| 상황 | 등급 | 규칙 |
| --- | --- | --- |
| 애플리케이션 지표 수집 실패 | CRITICAL | `prod 서비스 다운` |
| 업무 API 5xx 비율 급증 | CRITICAL | `5xx 응답 비율 급증` |
| Kakao, Apple 로그인 실패 | CRITICAL | `소셜 로그인 실패` |
| DB 커넥션 대기 지속 | CRITICAL | `DB 커넥션 대기` |
| JVM 힙, 디스크 사용률 90% 초과 | CRITICAL | `JVM 힙 사용률 90% 초과`, `앱 서버 디스크 90% 초과` |
| 업무 API 5xx 반복, 외부 서비스 동기 호출 실패 | WARNING | `5xx 응답 증가` |
| 메일, Web Push 같은 비동기 처리 실패 | WARNING | `ERROR 로그 발생` |
| DB 커넥션 타임아웃, 트랜잭션 롤백 | WARNING | `DB 커넥션 타임아웃`, `트랜잭션 롤백 발생` |
| 응답 지연, CPU와 디스크 사용률 증가, WARN 로그 급증 | WARNING | `평균 응답시간 1초 초과`, `CPU 사용률 85% 초과`, `앱 서버 디스크 80% 초과`, `WARN 로그 급증` |

임계값과 지속 시간은 [알림 규칙](../../deploy/monitoring/grafana/provisioning/alerting/rules.yaml)에서 관리합니다. 현재 값은 prod 트래픽을 기준으로 정한 초기값이며 운영하면서 조정합니다.

## 알림 규칙 관리

- Grafana의 데이터소스, 알림 규칙, 연락처와 알림 템플릿은 [provisioning 파일](../../deploy/monitoring/grafana/provisioning)로 관리합니다. Grafana UI에서는 수정하지 않습니다.
- 규칙을 추가하거나 변경하는 PR에는 [상황별 등급](#상황별-등급)을 함께 갱신합니다.
- Grafana는 provisioning 파일의 `$` 문자를 환경 변수로 해석합니다. 알림 템플릿과 annotation에서는 `$labels`, `$values` 대신 `.Labels`, `.Values`를 사용합니다.
- Slack 웹훅 URL은 모니터링 서버 `.env`의 `SLACK_MONITORING_WEBHOOK_URL`에 둡니다.
- 변경 사항은 [설정 변경](#설정-변경) 절차로 반영합니다.
