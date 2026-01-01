# FMI-BE (Find My Item - Backend)

Spring Boot 기반 분실물/습득물 서비스 백엔드. 

## 요구사항
- Java 17 
- Docker / Docker Compose

## 빠른 시작
1) 레포 클론
```
git clone https://github.com/find-my-item/FMI-BE.git
cd FMI-BE
```

2) 로컬 MySQL 기동
```
docker compose up -d
```

3) 애플리케이션 실행 
```
./gradlew bootRun
```
- 헬스체크: `GET http://localhost:8080/api/health` 또는 `GET http://localhost:8080/actuator/health`

## API 문서
서버 실행 후 아래 주소에서 API 명세를 확인할 수 있습니다.
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html` 

## 빌드/JAR/Docker 이미지
- 빌드
```
./gradlew clean build -x test
```
- 실행 JAR: `build/libs/*.jar`
- Docker 이미지 빌드
```
docker build -t fmi-be:local .
```

## Git 브랜치 전략 (Git Branch Strategy)
이 프로젝트는 Git Flow 전략을 따릅니다. 아래 규칙을 준수해주세요.

- 모든 작업은 이슈 기반 브랜치에서 진행합니다.
-  main, develop 브랜치에 직접 push/commit 금지, PR로만 머지합니다.
- 작업 브랜치는 develop에서 생성하며 이름은 타입/이슈번호 형식을 사용합니다. 예) feature/#1
- 의존 이슈가 있을 경우, 필요한 작업물이 있는 브랜치에서 분기합니다. 예) feature/#2 → feature/#1에서 분기
- 커밋 메시지는 Conventional Commits 양식을 사용합니다. 예) feat: 기능 추가