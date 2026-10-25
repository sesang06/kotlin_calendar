---
name: proto-api
description: .proto 메시지 계약과 gRPC/HTTP 전송 규칙. proto 작성·하위 호환·코드 생성, RPC↔HTTP 경로 매핑, 서버 이중 노출, 클라이언트 전송 선택, 에러·인증 매핑. API를 추가·변경할 때 먼저 읽는다.
---

# Proto 계약 + 전송

모든 메시지는 `proto/<도메인>/v<n>/*.proto`로만 정의한다 (DTO 손작성 금지, 생성 코드 커밋 금지). 예시: `proto/calendar/v1/event.proto`, `proto/auth/v1/auth.proto`.

## proto 작성

- `package calendar.v1;`, `option java_multiple_files = true;`, `option java_package = "com.kotlincalendar.proto.calendar.v1";`
- 서비스 `XxxApi`, 메시지 PascalCase, 필드 snake_case, enum 0번 `XXX_UNSPECIFIED`. RPC마다 Request/Response (Get/Create는 리소스 반환 허용).
- 시각 `google.protobuf.Timestamp` (`start_time`, `created_at`), 날짜 `string yyyy-MM-dd`, ID `string`(UUID).
- **하위 호환**: 필드 번호 재사용·타입 변경 금지, 삭제 시 `reserved`. 깨지는 변경은 `v2/`.
- `service` 위 주석에 `RPC | HTTP | 성공` 매핑을 적는다.
- 민감 값은 요청 바디로만. 소유자 ID는 요청으로 받지 않고 토큰에서 정하며, 남의 리소스는 `NOT_FOUND`.

## HTTP 매핑

조회 GET(쿼리/경로), 생성·수정 POST/PATCH + proto 바디. 바디는 `application/x-protobuf`. 성공: 조회 200, 생성 201, 삭제 204.

| RPC | HTTP |
|---|---|
| EventApi.ListEvents / GetEvent | `GET /api/events?date=&time_zone=` / `GET /api/events/{id}` |
| EventApi.CreateEvent / DeleteEvent | `POST /api/events` (201) / `DELETE /api/events/{id}` (204) |
| AuthApi.SignUp / Login / RefreshToken / Logout | `POST /api/auth/{signup(201),login,refresh,logout(204)}` |

## 서버: 같은 Service를 두 Handler로

HTTP(:8080, `EventHttpHandler`)와 gRPC(:50051, `EventGrpcHandler`)는 같은 Service·같은 매퍼를 쓴다. 상세는 `server`.

| DomainException | HTTP | gRPC |
|---|---|---|
| Validation | 400 | INVALID_ARGUMENT |
| NotFound | 404 | NOT_FOUND |
| Conflict | 409 | ALREADY_EXISTS |
| Unauthenticated | 401 | UNAUTHENTICATED |

인증: `Authorization: Bearer` (HTTP 헤더 / gRPC metadata), 검증은 Ktor Authentication·`AuthServerInterceptor` 한 곳. SignUp·Login·RefreshToken만 무인증.

## 클라이언트: 전송 선택

Repository는 `XxxRemoteDataSource` 인터페이스만 안다. `di/AppModule.kt`에서 `ApiConfig.transport`로 HTTP/gRPC 구현을 고른다 (기본 HTTP). `GrpcClientFactory`가 null인 플랫폼(Web, iOS)은 HTTP. 참고 구현: `client/shared/.../data/remote/{http,grpc}/`.

## 변경 순서

1. `.proto` 수정 → 2. 서버 `:server:generateProto`, 매퍼·HTTP·gRPC Handler + 테스트 둘 다 → 3. 클라이언트 `:client:shared:generateCommonMainProtos`, 매퍼·RemoteDataSource(HTTP·gRPC) + 테스트 → 4. 위 매핑 표·서비스 주석 갱신.
