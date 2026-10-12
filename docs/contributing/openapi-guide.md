# OpenAPI 작성 가이드

이 문서는 FI-BE의 SpringDoc OpenAPI와 Swagger UI에 표시할 API 설명을 작성하고 관리하는 방법을 안내합니다. API의 경로와 응답 계약은 [API 설계 기준](api-pattern.md), 개발 문서의 문체와 형식은 [문서 작성](documentation.md)을 따릅니다. 클라이언트가 사용하는 계약은 DOCS의 [API 스펙 문서 가이드](https://github.com/finditem/DOCS/blob/main/guide/API%20%EC%8A%A4%ED%8E%99%20%EB%AC%B8%EC%84%9C%20%EA%B0%80%EC%9D%B4%EB%93%9C.md)와 [공통 API 스펙](https://github.com/finditem/DOCS/blob/main/00-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/%EC%B0%BE%EC%95%84%EC%A4%98%21%20-%20%EC%9A%B4%EC%98%81%20%282%EC%B0%A8%20MVP%29/API%20%EC%8A%A4%ED%8E%99/%EA%B3%B5%ED%86%B5%20API%20%EC%8A%A4%ED%8E%99.md)에서 확인합니다.

## 분류와 이름

Swagger의 태그는 화면이나 Controller가 아니라 API가 다루는 업무 자원을 기준으로 정합니다. PR #572의 리뉴얼 기준 태그는 `Auth`, `User`, `Post`, `Place`, `Chat`, `Inquiry`, `Report`, `Block`, `Notice`, `Notification`, `System`입니다. 태그 설명과 표시 순서는 [`SwaggerConfig`](../../src/main/java/com/fmi/global/config/SwaggerConfig.java)에서 관리합니다.

태그의 추가, 이름, 소속과 분류 변경은 프론트엔드 담당자와 영향 및 변경 시점을 합의한 뒤 반영합니다. 새 API는 기존 태그 사용을 우선합니다.

## 주석 위치

새 API의 Swagger 주석은 해당 도메인의 `web`에 둡니다. 기존 기능에서 `web/swagger` 인터페이스를 사용하면 그 구조를 유지하고, Controller에 주석을 작성한 기능이면 해당 Controller의 방식을 따릅니다. `web/swagger` 파일을 새로 만들 때는 [도메인 경계](../architecture/domain-boundaries.md#web-계층)의 이름과 위치 기준을 따릅니다. 주석의 위치를 통일하기 위한 대규모 코드 이동은 별도 작업으로 다룹니다.

`@Tag`는 업무 분류를, `@Operation`의 `summary`는 사용자가 구분할 수 있는 동작을 표현합니다. `description`에는 호출 목적, 중요한 입력 조건과 결과를 적습니다. 구현 절차나 정책서의 상세 판단 규칙을 복사하지 않습니다. 요청 DTO와 응답 DTO의 필드 설명은 실제 JSON 이름, 타입, 필수 여부, 허용 값과 단위를 확인해 작성합니다.

예를 들어 [`PostController`](../../src/main/java/com/fmi/domain/post/web/controller/PostController.java)는 `Post` 태그 아래에 게시글 작성과 목록 조회를 둡니다. 운영진 API도 Controller 이름이 아니라 자원에 맞는 태그를 고르는 사례는 [`AdminController`](../../src/main/java/com/fmi/domain/admin/web/controller/AdminController.java)에서 확인합니다.

**적용 전**

```java
@Tag(name = "Admin")
```

**적용 후**

```java
@Operation(tags = {"Inquiry"}, summary = "운영진 회원 문의 목록 조회")
```

새 API의 설명과 응답 조건은 구현과 관련 스펙에서 확인합니다.

## 응답과 오류

`@ApiResponses`에는 해당 API가 실제 반환하는 성공 및 오류의 HTTP 상태와 조건을 적습니다. 공통 응답 형식과 오류 코드의 기준은 DOCS의 공통 API 스펙을 따릅니다. 기존 구현이 공통 기준과 다르면 Swagger에는 현재 동작을 정확히 설명하고, 계약 변경은 별도 결정으로 다룹니다. `200` 성공만 표시하거나 모든 API에 같은 오류 목록을 복사하지 않습니다.

인증이 필요 없는 API와 선택 인증 API는 전역 Bearer 설정이 실제 요청 조건으로 오해되지 않는지 확인합니다. 쿠키와 Bearer 헤더의 현재 지원 범위는 DOCS의 공통 API 스펙을 확인합니다.

## 변경과 확인

HTTP 계약이 바뀌면 Controller, DTO, 관련 테스트, Swagger 주석과 DOCS API 스펙을 함께 확인합니다. 특히 경로와 메서드, 인증, 요청 필드, 응답 형식, HTTP 상태, 오류 코드, 목록 커서를 비교합니다.

로컬 실행 후 [Swagger UI](http://localhost:8080/swagger-ui/index.html) 또는 `/v3/api-docs`에서 태그 소속, 설명, 요청과 응답 모델을 확인합니다. 태그를 바꿨다면 해당 API가 기존 분류에서 사라지고 새 분류에 나타나는지 확인하고, 프론트엔드 담당자와 합의한 결과를 PR에 적습니다. 문서만 바꿨을 때도 실제 API 동작이 달라졌다고 주장하지 않습니다.
