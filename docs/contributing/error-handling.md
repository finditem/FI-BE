# Error Handling

오류는 결과를 결정할 책임이 있는 경계에서 해석합니다. 오류 코드와 HTTP 매핑은 도메인 구현과 API 계약을 확인합니다.

| 경계 | 다룰 내용 | 기준 |
| --- | --- | --- |
| `web` | 필수 입력과 형식 등 요청 계약 | [검증의 적용 경계](../architecture/domain-boundaries.md#요청값-검증) |
| 생성 Validator | 권한, 중복, 상태 등 Entity 생성 업무 조건의 거절 | [생성 검증 기준](../architecture/domain-boundaries.md#생성-검증과-객체-생성) |
| Converter, Entity 생성자와 Builder | 생성 업무 검증을 반복하지 않고 검증된 값으로 생성 | [생성 절차](../architecture/domain-boundaries.md#생성-검증과-객체-생성) |
| 순수 Policy | 여러 객체와 값을 함께 보는 업무 판단의 거절 | [Policy의 책임](../architecture/domain-boundaries.md#service와-policy의-책임) |
| `service/internal` 협력 객체 | 분리한 조회, 검증과 외부 연동의 실패 | [협력 객체의 책임](../architecture/domain-boundaries.md#service와-policy의-책임) |
| `repository` | 부재를 반환 계약으로 표현하고 기술 예외 전달 | [Repository 분리 기준](../architecture/repository-boundaries.md#책임과-데이터-소유권) |
| `service` | 권한, 조회 부재의 업무 의미, 트랜잭션과 외부 실패 뒤 결과 | [Service의 책임](../architecture/domain-boundaries.md#service와-policy의-책임) |

Entity 생성 업무 조건의 오류는 Validator에서 판단합니다. 상태 변경 검증과 VO 자체 검증의 위치는 별도 논의 대상으로 남기며, 이 생성 정책으로 확정하지 않습니다. 같은 검증을 여러 계층에 복제하지 않고 오류 코드와 HTTP 변환은 기존 API 계약을 따릅니다.

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
