# Monitoring

기술 구성은 [Stack](../architecture/stack.md), 실제 컨테이너 설정은 [모니터링 Docker Compose](../../deploy/monitoring/docker-compose.yml)에서 관리합니다.

## 수집과 접근 경로

- Grafana Alloy는 애플리케이션 서버의 로그를 모니터링 서버의 Loki로 전송합니다.
- Prometheus는 애플리케이션 서버의 내부 메트릭 수집 경로에 접근합니다.
- Grafana와 Prometheus의 호스트 포트는 localhost에 바인딩합니다. 운영자는 SSM 포트 포워딩으로 접근합니다.
- Loki는 애플리케이션 서버의 로그 전송을 받아야 하므로 localhost로 제한하지 않습니다. Security Group에서 로그 전송 서버만 허용합니다.

서버 셸과 모니터링 도구의 접속 절차는 팀 Confluence 운영 문서에서 관리합니다. 공개 개발 문서에 운영 접근 주소와 네트워크 세부 사항을 복사하지 않습니다.

## 설정 변경

Grafana 포트 바인딩을 변경할 때 데이터 볼륨을 유지하고 Grafana 컨테이너만 재생성합니다. 볼륨을 삭제하는 명령을 사용하지 않습니다. 변경 후 SSM 터널의 Grafana 접속과 상태 API, Prometheus 수집 대상, Loki 로그 유입을 확인합니다. 실패하면 이전 Compose 설정으로 Grafana 컨테이너를 다시 생성합니다.

애플리케이션 서버의 SSH, CI/CD와 Nginx 접근 정책은 이 모니터링 접속 설정과 별도로 관리합니다.
