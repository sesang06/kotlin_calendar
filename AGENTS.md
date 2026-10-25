# kotlin_calendar

Kotlin 단일 언어로 구성된 캘린더 서비스 모노레포.

## 구조

```
kotlin_calendar/
├── proto/                   # 클라이언트·서버 공통 메시지 계약 (.proto) — 단일 소스
├── client/                  # Kotlin Multiplatform (Compose Multiplatform)
│   ├── shared/              # 공통 로직: domain, data(Wire proto, HTTP/gRPC), presentation(MVVM)
│   ├── androidApp/          # Android 앱 진입점
│   ├── iosApp/              # iOS 앱 진입점 (Xcode 프로젝트, macOS에서만 빌드)
│   ├── desktopApp/          # Windows(JVM Desktop) 진입점 — MSI/EXE 패키징
│   └── webApp/              # Web(Wasm) 진입점
├── server/                  # Ktor 서버: Handler(HTTP·gRPC) → Service → Repository
│   └── src/main/resources/application.yaml
├── gradle/libs.versions.toml
├── .editorconfig            # ktlint 규칙
├── .agents/skills/          # 에이전트 스킬
└── .github/
    ├── workflows/ci.yml     # ktlint + 서버/클라이언트 테스트
    └── pull_request_template.md
```

## 아키텍처 결정

| 주제 | 결정 |
|---|---|
| 클라이언트 | Kotlin Multiplatform + Compose Multiplatform. 공통 로직은 `client/shared`에 MVVM |
| 서버 | Ktor. Handler → Service → Repository 단방향 |
| 메시지 | 전부 `proto/`의 `.proto`로 정의. 서버는 protoc + grpc-kotlin, 클라이언트는 Wire로 생성 |
| 통신 | gRPC(:50051) 또는 HTTP(:8080, `application/x-protobuf`). 같은 Service를 두 Handler가 노출. 웹은 HTTP 전용 |
| DB | Supabase(PostgreSQL). `DatabaseProvider` 주입으로 H2 교체 가능, Repository는 Exposed |
| DI | 서버·클라이언트 모두 Koin |
| 시간 | `kotlin.time.Instant/Clock`, 날짜는 `kotlinx-datetime`. proto에서는 `google.protobuf.Timestamp` |
| 테스트 | 서버 JUnit5 + MockK, 클라이언트 commonTest + Fake + Turbine |
| 린트 | ktlint (`ktlint_official`) |
| CI | GitHub Actions |
| 웹 배포 | Vercel 정적 호스팅(Wasm) + `api/proxy.js`가 `/api/*`를 Ktor 서버로 전달 (`docs/vercel-deployment.md`) |
| 협업 | 피처별 worktree + PR, 역할별 리뷰 에이전트(사용자 요청 시) |
| 저장소 | https://github.com/sesang06/kotlin_calendar (public) |

## 작업 흐름 (필수)

코드가 바뀌는 모든 작업은 이 순서를 따른다. `main`에 직접 커밋하지 않는다.

1. **피처 분할** — 요청을 독립적으로 머지 가능한 피처로 나누고 목록을 사용자에게 보여준다 (`feature-worktree`).
2. **worktree + 병렬 실행** — 피처마다 브랜치와 `../kotlin_calendar-worktrees/<브랜치>` worktree를 만들고, 독립 피처는 **피처마다 에이전트를 하나씩 동시에** 띄워 진행한다. 순차 처리하지 않는다 (병렬로 못 하는 이유가 있으면 사용자에게 그 이유를 말한다). 의존 피처는 선행 피처 첫 커밋 위에 stacked로 바로 시작한다.
3. **PR** — ktlint·테스트 검증 후 피처마다 PR 하나를 연다 (`pull-request`, `.github/pull_request_template.md`).
4. **리뷰 (선택)** — 리뷰 에이전트는 **사용자가 요청했을 때만** 띄운다. 요청이 없으면 PR을 연 뒤 리뷰 에이전트 없이 멈추고, 필요하면 리뷰를 요청할 수 있다고 한 줄로 알린다. 요청을 받으면 PR을 만든 에이전트는 리뷰하지 않고, 변경 경로에 따라 역할별(architecture / testing / contract / security-config) **별도 리뷰 에이전트**를 새 컨텍스트로 띄워 `pr-review` 스킬로 리뷰한다.
5. **반영·머지** — 리뷰(에이전트·사람)의 Blocker/Major를 새 커밋으로 반영하고, 리뷰 에이전트가 낸 지적이면 해당 역할에 재리뷰를 요청한다. 머지는 사용자가 하거나 명시적으로 지시할 때만.

## 스킬

스킬은 `.agents/skills/<이름>/SKILL.md`에 있다. 작업 종류에 맞는 스킬을 먼저 읽고 따른다.

| 영역 | 스킬 | 언제 |
|---|---|---|
| 흐름 | `feature-worktree` | 작업 시작: 피처 분할, 브랜치·worktree 생성, stacked 처리, 정리 |
| 흐름 | `pull-request` | 검증 → push → PR 생성, (요청 시) 리뷰 요청, 반영, 머지 조건 |
| 흐름 | `pr-review` | 리뷰 에이전트(사용자 요청 시에만): 역할 배정, 체크리스트, 심각도, `gh pr review` 형식 |
| 공통 | `proto-contract` | `.proto` 작성·변경, 코드 생성, gRPC↔HTTP 경로 매핑 |
| 공통 | `api-transport` | gRPC/HTTP 전송 방식, 서버 이중 노출, 클라이언트 전송 선택 |
| 공통 | `ktlint` | 코드 스타일, `ktlintCheck`/`ktlintFormat` |
| 공통 | `github-actions-ci` | CI 워크플로 수정, CI 실패 조사 |
| 클라이언트 | `kmp-project-structure` | 모듈·타깃·Gradle·플랫폼 진입점·expect/actual |
| 클라이언트 | `kmp-mvvm` | 화면, UiState/Action/Effect, ViewModel |
| 클라이언트 | `kmp-data-layer` | 도메인 모델, Repository, RemoteDataSource, Koin DI |
| 클라이언트 | `kmp-testing` | commonTest에서 ViewModel·Repository·RemoteDataSource 테스트 |
| 서버 | `ktor-server-structure` | 서버 모듈 구조, Application, 플러그인, DI 배선 |
| 서버 | `ktor-handler` | HTTP·gRPC Handler, proto↔도메인 매퍼, 상태 코드 |
| 서버 | `ktor-service` | 비즈니스 로직·검증·트랜잭션 경계 |
| 서버 | `ktor-repository` | 벤더 중립 Exposed 데이터 접근 |
| 서버 | `server-database` | Supabase 연결, DatabaseProvider 주입, 환경변수 |
| 서버 | `ktor-mockk-testing` | MockK 단위 테스트, testApplication·InProcess gRPC·H2 테스트 |

## 공통 규칙

- 서버 계층 의존은 **Handler → Service → Repository** 한 방향만 허용.
- 클라이언트 공통 로직은 `client/shared`에 두고 MVVM을 따른다. 플랫폼 진입점에는 로직을 두지 않는다.
- API를 바꿀 때는 **`.proto`부터** 수정하고 서버 HTTP·gRPC Handler와 클라이언트 RemoteDataSource를 함께 갱신한다.
- DTO·메시지 클래스를 손으로 만들지 않는다 (생성 코드 사용, 생성 코드는 커밋하지 않음).
- DB 접속 정보 등 비밀값은 커밋하지 않는다 (환경변수 / GitHub Secrets).
- 기능을 추가하면 테스트도 함께 추가한다. 테스트는 외부 서비스에 접속하지 않는다.
- 커밋 전 `./gradlew ktlintCheck` 통과.

## 자주 쓰는 명령

```bash
./gradlew ktlintCheck ktlintFormat               # 린트 / 자동 수정
./gradlew :server:test                           # 서버 테스트
DB_PROVIDER=h2 ./gradlew :server:run             # 서버 실행 (로컬 인메모리 DB)
./gradlew :server:run                            # 서버 실행 (Supabase, SUPABASE_DB_* 필요)
./gradlew :client:shared:desktopTest             # 공통 로직 테스트 (가장 빠름)
./gradlew :client:shared:allTests                # 공통 로직 전체 타깃 테스트
./gradlew :client:desktopApp:run                 # Windows 데스크톱 실행
./gradlew :client:webApp:wasmJsBrowserDevelopmentRun  # 웹 실행 (:8081)
./gradlew :client:androidApp:installDebug        # Android 설치
```
