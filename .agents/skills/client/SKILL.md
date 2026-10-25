---
name: client
description: KMP 클라이언트 규칙. 모듈·타깃·소스셋, expect/actual, MVVM(UiState/Action/Effect/ViewModel/Screen), domain/data 계층, RemoteDataSource, Koin. client/ 코드를 추가·수정할 때 읽는다.
---

# KMP 클라이언트

## 모듈

`client/shared`(androidTarget, iosArm64/iosSimulatorArm64, `jvm("desktop")`, `wasmJs { browser() }`)에 도메인·데이터·ViewModel·UI 전부. `androidApp`/`iosApp`/`desktopApp`/`webApp`은 `App()` 호출과 Koin 시작만 (**로직 금지**).

- 패키지 루트 `com.kotlincalendar.client`. 의존성은 `libs.versions.toml` → `libs.*` (버전 직접 기입 금지).
- 플랫폼 차이는 Koin `platformModule` 주입 우선, `expect/actual`은 엔진·`platformModule` 같은 작은 것만.
- commonMain 금지: `java.*`/`android.*`, `runBlocking`, `Dispatchers.IO`(→ `Default`), 파일 시스템.
- 시간 `kotlin.time.Instant/Clock`(opt-in `ExperimentalTime`), 날짜 `kotlinx.datetime.LocalDate`.

## MVVM (`presentation/<feature>/`)

참고: `presentation/calendar/`, `presentation/event/create/`.

- 파일: `XxxUiState`(불변 data class 하나), `XxxAction`(sealed), `XxxEffect`(sealed, 1회성), `XxxViewModel`, `XxxScreen`.
- ViewModel: `androidx.lifecycle.ViewModel` + `viewModelScope`, 공개 API는 `state: StateFlow`, `effects: Flow`(`Channel.BUFFERED`), `onAction()`만. Compose·플랫폼 import 금지, domain 인터페이스와 `Clock`만 주입.
- Screen: `XxxRoute`(koinViewModel, 상태 수집, Effect 처리) + `XxxScreen(state, onAction)`(순수 UI).
- 새 화면: Koin `viewModelOf(::XxxViewModel)` 등록, `App.kt` NavHost에 type-safe route 추가, ViewModel 테스트.

## domain / data

참고: `domain/`, `data/repository/EventRepositoryImpl.kt`, `data/remote/`, `data/mapper/`, `di/AppModule.kt`.

- domain은 Ktor·Wire·proto를 import하지 않는다. proto 메시지는 data 밖으로 나가지 않는다 (매퍼는 `data/mapper`).
- Repository는 `suspend fun …: Result<T>`, `CancellationException`은 다시 던진다 (`runCatchingCancellable`).
- RemoteDataSource는 인터페이스 + `http/`(Ktor, `application/x-protobuf`, `ADAPTER.decode`) + `grpc/`(Wire 생성 클라이언트). 선택은 `proto-api`.
- `platformModule`이 `ApiConfig`, `HttpClientEngine`, `GrpcClientFactory`를 제공한다.
