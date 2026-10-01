# Error Handling

오류는 결과를 결정할 책임이 있는 경계에서 해석합니다. 오류 코드와 HTTP 매핑은 도메인 구현과 API 계약을 확인합니다.

| 경계 | 다룰 내용 | 기준 |
| --- | --- | --- |
| `web` | 요청 형식과 Bean Validation | [Domain Boundaries](../architecture/domain-boundaries.md) |
| `service/internal` Validator | 업무 조건, 권한과 상태 검증 | [Domain Boundaries](../architecture/domain-boundaries.md) |
| `repository` | Entity, Projection, Optional 또는 빈 컬렉션 반환 | [Domain Boundaries](../architecture/domain-boundaries.md) |
| `service` | 조회 부재의 업무 의미, transaction, 외부 실패 뒤 결과 | [Domain Boundaries](../architecture/domain-boundaries.md) |

## As-is / To-be 예시

```java
// 피할 예시: Repository에서 업무 실패를 확정한다.
Post getRequiredPost(Long id) {
    return findById(id).orElseThrow(() -> new PostNotFoundException());
}
```

```java
// 권장 예시: Repository의 부재를 Service가 유스케이스에 맞게 해석한다.
Post post = postRepository.findById(postId)
        .orElseThrow(() -> new PostNotFoundException());
```

가상 예시입니다. 실제 예외와 HTTP 응답은 해당 유스케이스 계약을 따릅니다.

DB 제약 실패, S3 또는 Redis 실패를 사용자 오류로 바꿀지는 확인된 정책과 실제 원인을 기준으로 결정합니다. 실패 뒤 DB 상태와 외부 효과를 [Testing](testing.md)에 따라 검증합니다.
