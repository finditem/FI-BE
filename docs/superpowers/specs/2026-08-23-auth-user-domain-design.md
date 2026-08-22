# auth·user 도메인 행위 설계

## 목표

auth와 user 범위에서 `User`의 Setter 기반 상태 변경을 도메인 행위로 바꾸고, Service가 유스케이스 정책과 처리 순서를 드러내게 한다.

## 경계

- `AuthController`는 `/auth/**`와 비밀번호 검증·변경, 탈퇴 API를 함께 소유한다. URL과 응답은 변경하지 않는다.
- `UserController`는 내 정보·프로필·약관·활동 API를 유지한다.
- Service와 Controller를 유스케이스마다 기계적으로 분할하지 않는다. public Service 메서드가 유스케이스 흐름을 표현한다.
- `internal`은 복잡하거나 둘 이상의 유스케이스에서 재사용되는 협력자에만 도입한다.
- `Query` 계층과 이름은 만들지 않는다. 단순 조회는 Repository를 직접 사용한다.

## User 상태 전이와 테스트

`User`는 이메일 인증, 약관 동의, 프로필, 임시 비밀번호, 비밀번호 변경, 탈퇴, 소셜 재가입의 상태 전이를 소유한다. 암호화, Repository 조회, Redis·메일·S3 호출은 Service에 남는다. 상태 전이 메서드는 `now`를 인자로 받아 Spring과 Mockito 없이 단위 테스트한다.
