# 기능 추가

새 기능의 결과와 데이터 owner를 먼저 확인한다. 세부 구조는 [Decision Guide](../architecture/decision-guide.md), 테스트 범위는 [Testing](testing.md)를 따른다.

## 1. 확정된 조건과 질문을 나눈다

```text
확인된 조건: 게시글 작성 요청이 Post 도메인으로 들어온다.
확인이 필요한 조건: 같은 사용자가 동일한 사진을 다시 올리면 새 게시글인가, 중복 요청인가?
질문: 중복 업로드를 허용하나요? 허용하지 않는다면 판단 기준과 보관 기간은 무엇인가요?
영향: DB 제약, S3 객체 처리, 재시도 결과와 테스트가 달라진다.
```

이는 질문 작성 형식의 예시다. 중복 업로드 정책이 실제로 미확정이라는 뜻은 아니다. 관련 정책과 현재 구현을 확인한 뒤 필요한 질문만 한다.

## 2. 시작점부터 결과까지 적는다

```text
Post Controller의 HTTP 요청
  -> PostService의 업무 흐름과 transaction
  -> PostValidator의 업무 조건 검증
  -> PostRepository의 JPA 저장 또는 조회
  -> 외부 이미지 저장이 있다면 호출 순서와 실패 뒤 상태 확인
```

실제 코드에서 [`PostService`](../../src/main/java/com/fmi/domain/post/service/PostService.java), [`PostValidator`](../../src/main/java/com/fmi/domain/post/service/internal/PostValidator.java), [`PostRepository`](../../src/main/java/com/fmi/domain/post/repository/PostRepository.java)를 확인한다. 위 순서는 기능별 실제 호출 순서를 대신하지 않는다.

| 항목 | 변경 전에 적을 내용 |
| --- | --- |
| 입력과 출력 | 요청 DTO, 성공 결과, 오류 결과 |
| read와 write | 대상 테이블, 조회 조건, owner |
| transaction | DB 변경을 묶는 범위와 rollback 조건 |
| 외부 효과 | S3, Redis, HTTP, 알림 호출과 재시도 결과 |
| 마지막 방어선 | 권한, 유일성, 상태 전이, DB 제약 |

## 3. 코드와 테스트를 함께 바꾼다

요청 형식은 `web`의 Bean Validation으로, 업무 조건은 `service/internal`의 Validator로, 상태 변화는 `data` 객체의 의미 있는 메서드로 표현한다. 이 역할 구분은 [Domain Boundaries](../architecture/domain-boundaries.md)가 소유한다. 목록과 집계 조회에는 [Persistence](../architecture/persistence.md)의 QueryDSL 기준을 적용한다.

MySQL, transaction 또는 외부 연동 결과가 중요하다면 [Testing](testing.md)의 실제 통합 테스트를 작성한다. HTTP 계약이 바뀌면 현재 SpringDoc 설정과 해당 DTO를 확인한다. 커밋 전에는 [Contributing Overview](overview.md)의 순서를 따른다.
