# API 설계 예시

새 HTTP API를 설계할 때 요청과 응답을 먼저 아래 형식으로 적는다. 예시는 현재 [`PostController`](../../src/main/java/com/fmi/domain/post/web/controller/PostController.java)의 게시글 필터 검색을 바탕으로 한다. 클라이언트 계약의 공통 기준은 DOCS의 [공통 API 스펙](https://github.com/finditem/DOCS/blob/main/00-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/%EC%B0%BE%EC%95%84%EC%A4%98%21%20-%20%EC%9A%B4%EC%98%81%20%282%EC%B0%A8%20MVP%29/API%20%EC%8A%A4%ED%8E%99/%EA%B3%B5%ED%86%B5%20API%20%EC%8A%A4%ED%8E%99.md)을 따른다.

## 게시글 목록 조회 예시

| 항목 | 설계 내용 |
| --- | --- |
| 기능 | 조건에 맞는 게시글 목록을 커서 방식으로 조회 |
| 요청 | `GET /posts/search` |
| Swagger 태그 | `Post` |
| 인증 | 선택 인증. 로그인하지 않아도 조회 가능하며, 로그인하면 사용자별 정보를 반영 |
| 주요 입력 | `postType`, `postStatus`, `category`, `address`, `sortType`, `cursor`, `size` |
| 성공 | HTTP `200 OK`, `ApiResponse<PostPageResponse>` |
| 오류 | 현재 Swagger에는 `400`, `401`이 기재되어 있다. 발생 조건과 응답 코드는 구현을 확인해 개별 API 스펙에 적는다. |

```text
첫 요청: GET /posts/search?address=서울&sortType=LATEST&size=20
다음 요청: GET /posts/search?address=서울&sortType=LATEST&cursor=98&size=20
```

결과가 없을 때 응답의 핵심 구조는 다음과 같다. `postList`와 `postCount`는 기존 [`PostPageResponse`](../../src/main/java/com/fmi/domain/post/web/dto/response/PostPageResponse.java)의 필드다. 새 목록 API의 공통 필드명으로 복사하지 않는다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "postList": [],
    "postCount": 0,
    "nextCursor": null,
    "hasNext": false
  }
}
```

다음 페이지가 있으면 응답의 `nextCursor`를 다음 요청에 전달한다. 마지막 페이지에는 `hasNext=false`, `nextCursor=null`을 사용한다. 현재 조회 흐름은 [`PostQueryService`](../../src/main/java/com/fmi/domain/post/service/PostQueryService.java)와 [`PostRepositoryImpl`](../../src/main/java/com/fmi/domain/post/repository/PostRepositoryImpl.java)에서 확인한다.

## 새 API 설계에 사용할 틀

```text
기능과 결과:
Method와 Path:
Swagger 태그:
인증: 없음 / 선택 인증 / 로그인 / 운영진
요청: 위치, 필드, 필수 여부, 허용 값
성공: HTTP 상태, 응답 코드, JSON 필드, 빈 결과
오류: 발생 조건, HTTP 상태, 응답 코드
목록이면: 정렬, 첫 요청, 다음 커서, 마지막 페이지
반복 요청이면: 같은 요청의 두 번째 결과
근거: 정책서, DOCS API 스펙, 현재 Controller와 DTO
```

공통 응답, 오류 코드, 데이터 표현과 커서 기준은 DOCS의 공통 API 스펙을 연결하고 중복 정의하지 않는다. 현재 구현과 새 계약이 다르면 차이를 적고 합의한 뒤 코드, 테스트, DOCS API 스펙과 [Swagger 설명](openapi-guide.md)을 함께 바꾼다. Swagger 태그 변경은 프론트엔드의 API 요청 파일 구조에 영향을 주므로 [분류 변경 절차](openapi-guide.md#분류와-이름)를 먼저 따른다.
