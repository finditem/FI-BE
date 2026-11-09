# OpenAPI 작성 가이드

이 문서는 FI-BE의 SpringDoc OpenAPI와 Swagger UI에 표시할 API 설명을 작성하고 관리하는 방법을 안내합니다. API의 경로와 응답 계약은 [API 설계 기준](api-pattern.md), 개발 문서의 문체와 형식은 [문서 작성](documentation.md)을 따릅니다. 클라이언트가 사용하는 계약은 DOCS의 [API 스펙 문서 가이드](https://github.com/finditem/DOCS/blob/main/guide/API%20%EC%8A%A4%ED%8E%99%20%EB%AC%B8%EC%84%9C%20%EA%B0%80%EC%9D%B4%EB%93%9C.md)와 [공통 API 스펙](https://github.com/finditem/DOCS/blob/main/00-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/%EC%B0%BE%EC%95%84%EC%A4%98%21%20-%20%EC%9A%B4%EC%98%81%20%282%EC%B0%A8%20MVP%29/API%20%EC%8A%A4%ED%8E%99/%EA%B3%B5%ED%86%B5%20API%20%EC%8A%A4%ED%8E%99.md)에서 확인합니다.

## 분류와 이름

Swagger의 태그는 화면이나 Controller가 아니라 API가 다루는 업무 자원을 기준으로 정합니다. 현재 태그 이름과 API별 소속은 생성 명세 및 [API 구조 계약](../../src/test/resources/contracts/api-structure.txt)을 기준으로 확인합니다. 전역 태그 설정과 표시 순서는 [`SwaggerConfig`](../../src/main/java/com/fmi/global/config/SwaggerConfig.java)에서 관리합니다.

태그의 추가, 이름, 소속과 분류 변경은 프론트엔드 담당자와 영향 및 변경 시점을 합의한 뒤 반영합니다. 새 API는 기존 태그 사용을 우선합니다.

## 주석 위치

Controller의 API 설명은 해당 기능의 `web/swagger/[기능명]Swagger.java` 인터페이스에 작성합니다. Controller는 인터페이스를 구현하고 HTTP 매핑, 요청 바인딩, 검증과 인증 주입을 담당합니다. JSON 필드의 설명은 요청 및 응답 DTO에 둡니다. 파일 위치와 이름은 [도메인 경계](../architecture/domain-boundaries.md#web-계층)를 따릅니다.

Swagger 인터페이스를 옮기거나 분리할 때 API에 이미 적용된 태그와 그 순서를 유지합니다. 기존 태그가 Controller 이름을 바탕으로 자동 생성되는 경우에도 생성 명세를 기준으로 동일한 분류를 유지합니다.

## API별 작성 형식

| 항목 | 작성 기준 |
| --- | --- |
| 요약 | `@Operation.summary`에 대상과 동작을 짧게 적습니다. 예를 들어 `게시글 목록 조회`처럼 씁니다. |
| 설명 | `@Operation.description`에 호출 결과와 호출자가 알아야 할 조건을 적습니다. 요약만으로 충분한 경우 생략할 수 있습니다. 커서, 쿠키, SSE 등 별도 사용법이 있는 경우에는 조건과 호출 예를 적습니다. |
| 인증 | 인증 없음, 선택 인증, 로그인, `ROLE_ADMIN` 중 실제 요청 조건을 확인합니다. 공개 및 선택 인증 API는 `@SecurityRequirements`로 전역 Bearer 요구사항을 해제하고, 선택 인증에 따른 응답 차이 또는 필요한 쿠키는 설명에 적습니다. |
| 요청 | 위치, 필수 여부, 기본값, 허용 값과 단위를 확인합니다. Spring MVC와 DTO에서 생성되는 정보에 호출에 필요한 설명을 보충합니다. |
| 성공 응답 | 실제 HTTP 상태와 반환 구조를 적습니다. 일반 JSON 응답은 반환 타입에서 스키마를 추론하도록 둡니다. 응답 예시와 함께 반환 타입을 명시적으로 사용하려면 `useReturnTypeSchema = true`를 지정합니다. `ApiResponse<T>`의 내부 DTO를 전체 응답 스키마로 지정하지 않습니다. SSE나 응답 본문이 없는 경우 실제 미디어 타입과 본문 유무를 확인합니다. |
| 오류 응답 | 실제 발생 조건과 HTTP 상태, 응답 코드를 적습니다. 같은 상태의 오류는 응답 하나에 조건별로 정리합니다. 예를 들어 `AUTH409-NICKNAME_DUPLICATED: 이미 사용 중인 닉네임입니다.`처럼 씁니다. |

공통 응답 구조와 인증 수단은 DOCS의 공통 API 스펙을 따릅니다. 개별 API가 공통 기준과 다르면 현재 동작을 Swagger에 정확히 기록하고, 계약 변경은 별도 결정으로 다룹니다. 실제 반환하지 않는 오류나 모든 API에 동일한 오류 목록을 복사하지 않습니다.

## 변경과 확인

Controller, DTO, 보안 설정, 오류 처리와 관련 테스트를 개별 API의 근거로 사용합니다. `/v3/api-docs`에서 경로, 메서드, `operationId`, 태그와 표시 순서, 인증 조건, 요청과 응답 스키마를 확인합니다. 분리 작업은 변경 전후 명세를 비교하고 의도한 설명 및 스키마 정정만 반영됐는지 검토합니다. `ApiStructureContractTest`는 API별 경로, 메서드, `operationId`와 태그를 검증합니다.

## 배포 환경 접근

dev와 prod의 Swagger UI 및 OpenAPI 명세는 공개 도메인에서 차단합니다. SSM 포트 포워딩으로 애플리케이션 서버의 localhost 포트에 접속합니다. 접속 명령과 환경별 포트는 팀 Confluence 운영 문서에서 관리합니다. 개인 개발용 local 프로필의 Swagger 접근은 유지합니다. dev 프로필에서는 Swagger의 API 서버 주소를 `https://dev-api.finditem.kr`로 지정합니다. 화면과 명세는 SSM 터널로 읽고, `Try it out` 요청은 공개 dev 도메인으로 전송합니다. prod와 local 프로필에는 이 주소를 적용하지 않습니다.
