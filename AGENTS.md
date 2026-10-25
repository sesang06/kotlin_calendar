# kotlin_calendar

Kotlin 단일 언어 캘린더 서비스 모노레포. 저장소: https://github.com/sesang06/kotlin_calendar

```
proto/      # 클라이언트·서버 공통 .proto 계약 (단일 소스)
client/     # KMP + Compose Multiplatform: shared(공통 로직), androidApp, iosApp, desktopApp, webApp
server/     # Ktor: Handler(HTTP·gRPC) → Service → Repository
api/, scripts/, vercel.json   # 웹 배포 (docs/vercel-deployment.md)
.agents/skills/               # 스킬
```

| 주제 | 결정 |
|---|---|
| 클라이언트 | KMP + Compose Multiplatform, `client/shared`에 MVVM |
| 서버 | Ktor, Handler → Service → Repository 단방향 |
| 메시지·통신 | `.proto` 단일 소스(서버 protoc+grpc-kotlin, 클라 Wire). gRPC :50051 / HTTP :8080(`application/x-protobuf`), 웹은 HTTP 전용 |
| DB | Supabase(PostgreSQL), H2 교체 가능, Exposed + Flyway |
| DI / 시간 | Koin / `kotlin.time.Instant`, `kotlinx-datetime`, proto `Timestamp` |
| 테스트 / 린트 / CI | 서버 JUnit5+MockK, 클라 commonTest+Fake+Turbine / ktlint_official / GitHub Actions |

## 작업 흐름

코드가 바뀌면 `workflow` 스킬을 따른다: 피처 분할 → 피처별 브랜치·worktree → 검증 → PR. `main`에 직접 커밋하지 않는다. 리뷰 에이전트는 사용자가 요청할 때만(`pr-review`). 머지는 사용자 지시가 있을 때만.

## 스킬 (`.agents/skills/<이름>/SKILL.md`) — 작업에 필요한 것만 읽는다

| 스킬 | 언제 |
|---|---|
| `workflow` | 코드 변경 작업 시작, PR |
| `pr-review` | 사용자가 리뷰를 요청했을 때만 |
| `proto-api` | `.proto`·API·전송(gRPC/HTTP) 변경 |
| `server` | `server/` 코드 |
| `server-database` | DB 설정, `db/migration` |
| `server-testing` | `server/src/test` |
| `client` | `client/` 코드 |
| `client-testing` | `client/shared/src/commonTest` |
| `ci-lint` | ktlint 오류, `.github/workflows` |

## 공통 규칙

- API 변경은 `.proto`부터, 서버 HTTP·gRPC Handler와 클라이언트 RemoteDataSource를 함께 갱신.
- 생성 코드·DTO 손작성 금지, 생성 코드 커밋 금지. 비밀값 커밋 금지(환경변수/Secrets).
- 기능에는 테스트를 함께. 테스트는 외부 서비스에 접속하지 않는다.
- 커밋 전 `./gradlew ktlintCheck`.
- Gradle 출력은 길다. `-q` 또는 `2>&1 | tail -n 40`으로 줄여 보고, 실패 시 `build/reports`·`build/test-results`에서 필요한 부분만 본다.

## 명령

```bash
./gradlew ktlintCheck ktlintFormat
./gradlew :server:test
DB_PROVIDER=h2 ./gradlew :server:run
./gradlew :client:shared:desktopTest      # 가장 빠름
./gradlew :client:shared:allTests
./gradlew :client:desktopApp:run
./gradlew :client:webApp:wasmJsBrowserDevelopmentRun   # :8081
```
