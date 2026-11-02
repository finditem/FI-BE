# Repository 분리 기준

Repository의 업무별 구성, 명명과 기술별 구현 구조에 적용합니다. 조회 기술, 반환 타입, JPA 관계, DB 제약과 검증은 [Persistence](persistence.md)를 따릅니다.

## Repository 구성과 분리

Repository는 Service에서 정한 업무 구분을 기준으로 책임을 나누고 이름을 짓습니다. Entity 하나당 Repository 하나로 제한하거나 Service와 Repository를 반드시 1:1로 구성하지 않습니다. Service 구성은 [서비스 분리 기준](service-boundaries.md)을 따릅니다.

| 구분 | 책임 | 이름 예시 |
| --- | --- | --- |
| 기본 Repository | 공통 저장, 기본 Entity 조회, 상태 변경에 필요한 잠금 조회 | `PostRepository` |
| 업무별 Repository | 해당 업무의 조회 조건, 정렬, 조인과 결과 매핑 | `PostSearchRepository`, `PostMapRepository`, `PostHotRepository` |

새 데이터 접근 코드는 다음 순서로 배치합니다.

1. 해당 업무의 Repository가 있으면 그곳에 둡니다.
2. 공통 저장이나 기본 Entity 조회라면 기본 Repository에 둡니다.
3. 업무 고유의 조회가 모여 독립적인 책임을 이루면 별도 Repository로 분리합니다.

메서드 수, 쿼리 길이 또는 사용하는 기술만으로 업무를 분리하지 않습니다. Service마다 Repository를 만들거나 동일한 쿼리를 여러 Repository에 복제하지 않습니다. 업무별 조회와 공통 저장이 모두 필요한 Service는 각각의 Repository를 사용할 수 있습니다.

업무별 Repository는 필요한 Service에서 직접 사용합니다. 분리한 업무별 조회를 기본 Repository에 다시 모두 상속하거나 단순 위임으로 모으지 않습니다.

이 기준을 적용하기 위해 기존 Repository를 일괄 재구성하지 않습니다. 기존 영역의 변경은 [레거시 처리 기준](../../AGENTS.md#레거시-코드와-리팩터링)을, 구조를 변경하는 작업은 [Refactoring](../contributing/refactoring.md)을 따릅니다.

## 기술별 구현 방식

저장과 조회 기술의 선택은 [Persistence](persistence.md#조회-기술-선택)를 따릅니다. Repository를 분리한다는 이유로 저장 기술을 바꾸지 않습니다.

### Spring Data JPA

Repository의 책임에 필요한 기능만 노출합니다. 모든 업무별 Repository가 `JpaRepository`를 상속할 필요는 없습니다. `org.springframework.data.repository.Repository`는 기본 CRUD 메서드를 모두 노출하지 않는 마커 인터페이스입니다.

### QueryDSL

QueryDSL로 구현하는 메서드는 해당 Repository의 `*Custom` 인터페이스에 선언합니다. 구현체는 Custom 인터페이스 이름에 `Impl`을 붙여 작성합니다.

다음은 구성 예시이며 현재 클래스의 배치를 나타내지 않습니다.

```java
public interface PostSearchRepository
        extends Repository<Post, Long>, PostSearchRepositoryCustom {}

public interface PostSearchRepositoryCustom {
    // 검색 메서드 선언
}

@RequiredArgsConstructor
public class PostSearchRepositoryCustomImpl implements PostSearchRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    // 검색 메서드 구현
}
```

QueryDSL 구현은 업무별로 나눕니다. 검색, 지도와 인기 게시글의 Custom 인터페이스를 기본 `PostRepository`에 모두 합치지 않습니다. 같은 업무의 파생 쿼리와 QueryDSL은 해당 업무 Repository에서 함께 노출할 수 있습니다.

### JDBC와 그 밖의 데이터 접근 기술

Service가 사용할 업무별 Repository 인터페이스를 선언하고 기술별 구현체가 이를 구현합니다. 인터페이스 이름은 업무를, 구현체 이름은 사용 기술을 표현합니다. Service는 인터페이스에 의존하며 JDBC 전용 타입이나 구현체에 직접 의존하지 않습니다.

다음은 JDBC를 선택한 경우의 구성 예시이며 현재 통계 구현의 기술 변경을 요구하지 않습니다.

```text
PostStatisticsRepository
  업무에 필요한 메서드 선언

PostStatisticsJdbcRepository
  implements PostStatisticsRepository
  JDBC로 구현
```

같은 인터페이스에 구현체가 여러 개라면 사용할 구현체를 명시적으로 선택합니다. 여러 기술을 사용한다는 이유만으로 같은 업무 계약을 중복 정의하지 않습니다.

### Redis

- Redis 저장과 조회 구현은 소유 업무의 `repository/redis`에 둡니다. 여러 도메인의 구현을 공통 Redis 패키지로 모으지 않습니다.
- Service에 Redis 접근이 섞여 있으면 키 구성, TTL 설정, Redis 명령과 Lua 실행, 저장 결과 매핑을 저장소 구현으로 분리합니다. 업무 판단과 유스케이스 처리 순서는 Service에 둡니다.
- 외부 연동이 소유하는 Redis 데이터 접근은 해당 `external` 연동 영역의 `repository/redis`에 둡니다.
- 저장소 전용 예약 정보와 처리 결과 타입은 해당 저장소가 소유하며 중첩 타입으로 둘 수 있습니다. 서비스 실행 결과 타입과 구분합니다.
- Redis 구현을 배치하기 위해 기존 JPA Repository를 `repository/jpa`로 이동하지 않습니다.

## 책임과 데이터 소유권

Repository는 데이터 접근과 결과 매핑을 담당합니다. 조회 결과가 없으면 `Optional`, 빈 컬렉션 등 반환 계약으로 표현하며, 그 이유로 업무 예외를 만들지 않습니다. 부재의 의미와 업무 오류는 Service 또는 `service/internal`의 Reader가 판단합니다. Repository는 데이터 접근 중 발생한 기술 예외를 전달하거나 영속성 계층의 예외로 변환할 수 있습니다.

Repository를 업무별 또는 기술별로 분리해도 데이터 소유권과 트랜잭션 경계는 달라지지 않습니다. 다른 도메인의 조회와 변경은 [Domain Boundaries](domain-boundaries.md)를 따릅니다. 여러 Repository를 사용하는 유스케이스의 트랜잭션은 Service가 관리합니다.
