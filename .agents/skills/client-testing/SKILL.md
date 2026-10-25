---
name: client-testing
description: 클라이언트 commonTest 규칙. ViewModel·Repository·RemoteDataSource·매퍼 테스트, Fake, 테스트 디스패처, Turbine, MockEngine. client/shared 테스트를 작성할 때 읽는다.
---

# 클라이언트 테스트

위치: `client/shared/src/commonTest/kotlin/com/kotlincalendar/client/` (프로덕션과 같은 패키지, Fake는 `fake/`).

| 대상 | 방법 | 참고 |
|---|---|---|
| ViewModel | Fake Repository + `StandardTestDispatcher` + Turbine | `presentation/calendar/CalendarViewModelTest.kt` |
| Repository | Fake RemoteDataSource | `data/repository/EventRepositoryImplTest.kt` |
| HTTP RemoteDataSource | Ktor `MockEngine`이 proto 바이트 응답 | `data/remote/http/HttpEventRemoteDataSourceTest.kt` |
| gRPC RemoteDataSource | Fake `EventApiClient` | `data/remote/grpc/GrpcEventRemoteDataSourceTest.kt` |

## 규칙

- **MockK 금지**(JVM 전용) → `fake/`의 수작업 Fake (호출 기록 + 응답 설정 필드).
- ViewModel: `Dispatchers.setMain(StandardTestDispatcher())` / `resetMain()`, `runTest(dispatcher)` + `advanceUntilIdle()`. Effect는 `effects.test { awaitItem() }`.
- 시간은 `FixedClock`.
- 테스트 함수명은 영문 camelCase/underscore (iOS·wasm에서 백틱 한글 이름이 깨질 수 있음).

```bash
./gradlew :client:shared:desktopTest   # 가장 빠름
./gradlew :client:shared:allTests
```
