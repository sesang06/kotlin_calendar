---
name: proto-contract
description: kotlin_calendar 클라이언트·서버 통신 메시지를 Protocol Buffers(.proto)로 정의하는 규칙. 루트 proto/ 디렉터리 구조, 메시지·서비스 작성 스타일, 하위 호환 규칙, 서버(protoc + grpc-kotlin)와 클라이언트(Wire) 코드 생성, gRPC 메서드↔HTTP 경로 매핑이 필요할 때 사용. API를 추가·변경하거나 .proto 파일을 만질 때 가장 먼저 읽는다.
---

# Proto 계약

클라이언트와 서버가 주고받는 **모든 메시지는 `.proto`로만 정의**한다. Kotlin DTO 클래스를 손으로 만들지 않는다.

## 위치와 코드 생성

```
proto/                         # 단일 계약 소스 (Gradle 모듈 아님)
├── auth/v1/
│   └── auth.proto             # AuthApi: 회원가입·로그인·토큰 재발급·로그아웃
└── calendar/v1/
    └── event.proto            # EventApi: 일정 생성·조회·삭제
```

| 쪽 | 생성기 | 설정 위치 | 생성 패키지 |
|---|---|---|---|
| 서버 | `com.google.protobuf` 플러그인 + protoc + `protoc-gen-grpc-java` + `protoc-gen-grpc-kotlin` | `server/build.gradle.kts`의 `sourceSets.main.proto.srcDir(rootProject.file("proto"))` | `java_package` (Java 빌더 클래스 + `XxxGrpcKt` 코루틴 스텁) |
| 클라이언트 | Wire (`com.squareup.wire`), `rpcRole = "client"` | `client/shared/build.gradle.kts`의 `wire { sourcePath { srcDir(rootProject.file("proto")) } }` | commonMain에 KMP 메시지 클래스 + `GrpcXxxClient` |

생성 코드는 `build/generated/` 아래에 생기며 **커밋하지 않고 ktlint 검사에서도 제외**된다.

## 파일 규칙

```proto
syntax = "proto3";

package calendar.v1;                                   // <도메인>.v<major>

option java_multiple_files = true;
option java_package = "com.kotlincalendar.proto.calendar.v1";
```

- 디렉터리 = 패키지: `proto/calendar/v1/*.proto` ↔ `package calendar.v1;`
- 파일 하나에 하나의 리소스(서비스 + 그 메시지). 공용 메시지는 `common.proto`.
- 메시지·서비스·enum 이름은 `PascalCase`, 필드는 `snake_case`, enum 값은 `UPPER_SNAKE_CASE` + 0번은 `XXX_UNSPECIFIED`.
- RPC마다 전용 `XxxRequest` / `XxxResponse` 메시지를 둔다 (리소스 자체를 반환하는 Get/Create는 리소스 메시지 반환 허용).
- 서비스 이름은 `XxxApi` (서버의 도메인 `XxxService` 클래스와 이름 충돌 방지).

## 타입 규칙

| 의미 | proto 타입 | 비고 |
|---|---|---|
| 시각 | `google.protobuf.Timestamp` | `import "google/protobuf/timestamp.proto";` 사용. 필드명은 `start_time`, `end_time`, `created_at` 등 (`_epoch_millis` 접미사 사용 금지) |
| 날짜 | `string` (`yyyy-MM-dd`) | `kotlinx.datetime.LocalDate.parse` |
| ID | `string` | UUID 문자열 |
| 선택 값 | `optional` 필드 | proto3 기본값과 "없음" 구분 필요할 때만 |
| 에러 (HTTP) | `ErrorResponse { string message }` | gRPC는 `Status` 사용 |

## 하위 호환 (필수)

1. **필드 번호는 절대 재사용·변경하지 않는다.** 지운 필드는 `reserved 3; reserved "old_name";`.
2. 필드 타입 변경 금지. 필요하면 새 필드를 추가하고 옛 필드는 deprecated.
3. 깨지는 변경이 필요하면 `calendar/v2/`를 새로 만들고 v1과 한동안 병행.
4. enum 0번 값의 의미를 바꾸지 않는다.

## gRPC ↔ HTTP 매핑

같은 서비스를 두 가지 전송 방식으로 연다 (`api-transport` 스킬). 새 RPC를 추가하면 이 표 형식으로 HTTP 경로를 정하고, 서비스 위 주석에 적는다.

| RPC | HTTP | 요청 바디 | 응답 |
|---|---|---|---|
| `ListEvents` | `GET /api/events?date=yyyy-MM-dd&time_zone=Asia/Seoul` | 없음 (쿼리, `time_zone` 생략 시 UTC) | `ListEventsResponse` |
| `GetEvent` | `GET /api/events/{id}` | 없음 | `Event` |
| `CreateEvent` | `POST /api/events` | `CreateEventRequest` | `201` + `Event` |
| `DeleteEvent` | `DELETE /api/events/{id}` | 없음 | `204` |
| `AuthApi.SignUp` | `POST /api/auth/signup` | `SignUpRequest` | `201` + `SignUpResponse` |
| `AuthApi.Login` | `POST /api/auth/login` | `LoginRequest` | `LoginResponse` |
| `AuthApi.RefreshToken` | `POST /api/auth/refresh` | `RefreshTokenRequest` | `RefreshTokenResponse` |
| `AuthApi.Logout` | `POST /api/auth/logout` | `LogoutRequest` | `204` |

- 비밀번호·토큰 같은 민감 값은 **요청 바디로만** 보낸다 (URL·쿼리 금지). 응답 메시지에 비밀번호·해시를 담지 않는다.
- 매핑 정보는 `.proto`의 `service` 위 주석에도 둔다 (생성 코드를 보는 사람도 알 수 있게). 주석에는 RPC, HTTP 메서드+경로, 성공 상태 코드+응답 메시지가 모두 들어가야 하며, 열은 합쳐서 줄여도 된다 (예: `RPC | HTTP | 성공` 3열).
- 소유권이 있는 리소스는 소유자 ID를 요청 메시지로 받지 않는다. 서버가 access token에서 정하고, 남의 리소스는 `NOT_FOUND`로 숨긴다.
- HTTP 바디는 `Content-Type: application/x-protobuf` (protobuf 바이너리).
- 조회는 GET + 쿼리/경로 파라미터, 생성·수정은 POST/PATCH + proto 바디.

## 변경 체크리스트

1. `.proto` 수정 (호환 규칙 확인)
2. 서버: `./gradlew :server:generateProto` → 매퍼·Handler(HTTP, gRPC) 갱신
3. 클라이언트: `./gradlew :client:shared:generateCommonMainProtos` → 매퍼·RemoteDataSource 갱신
4. 위 매핑 표와 서비스 주석 갱신
5. 양쪽 테스트 실행
