# 애플리케이션 아키텍처

## 기술 스택

|        분류       |                            기술                              |
|-------------------|--------------------------------------------------------------|
| 언어 / 프레임워크 | Java 17, Spring Boot 3.5                                     |
| ORM / DB          | Spring Data JPA, QueryDSL, MySQL, Flyway                     |
| 인증              | Spring Security, JWT, Redis                                  |
| 실시간            | WebSocket (채팅)                                             |
| 스토리지          | AWS S3 (이미지)                                              |
| 알림              | 이메일, Web Push (VAPID)                                     |
| 모니터링          | Actuator, Prometheus, Grafana                                |
| 로깅              | Grafana Alloy, Loki, Logstash 포맷(JSON)                     |
| 인프라 / 배포     | AWS (EC2, RDS, ECR), Nginx, Docker, GitHub Actions           |
| 문서              | SpringDoc OpenAPI (Swagger)                                  |

## 아키텍처

업무 경계와 실행 흐름은 [Architecture Overview](overview.md), 구체적인 계층 책임은 [Domain Boundaries](domain-boundaries.md)를 따른다.
