---
name: server-testing
description: 서버 테스트 규칙. MockK Service 테스트, testApplication HTTP Handler 테스트, InProcess gRPC Handler 테스트, H2 Repository 테스트. server/src/test를 작성할 때 읽는다.
---

# 서버 테스트

테스트는 프로덕션과 같은 패키지에 `<대상>Test`. **기존 테스트를 템플릿으로 복사해서 쓴다.**

| 대상 | 모킹 | 참고 테스트 (`server/src/test/kotlin/com/kotlincalendar/server/`) |
|---|---|---|
| Service | Repository (MockK) | `event/service/EventServiceTest.kt` |
| HTTP Handler | Service (MockK) | `event/handler/http/EventHttpHandlerTest.kt` |
| gRPC Handler | Service (MockK) | `event/handler/grpc/EventGrpcHandlerTest.kt` |
| Repository | 없음, H2 + Flyway | `event/repository/ExposedEventRepositoryTest.kt` |
| DB 선택·마이그레이션 | 없음 | `common/database/*Test.kt` |

## 규칙

- 바로 아래 계층만 모킹. Supabase·외부 네트워크 접속 금지.
- suspend는 `coEvery`/`coVerify`. 기본 strict `mockk<T>()`. 캡처는 `slot`, 미호출은 `coVerify(exactly = 0)`.
- 시간·ID는 고정값 주입 (`object : Clock { … }`, `idGenerator = { "id-1" }`).
- HTTP Handler 테스트는 `environment { config = MapApplicationConfig() }`로 application.yaml 로드를 막는다.
- 새 엔드포인트는 HTTP·gRPC Handler 테스트 **둘 다**. 검증: 상태 코드, 응답 proto, Service 인자, 잘못된 입력 시 Service 미호출.
- Service: 정상 경로 + 각 검증 실패(예외 타입 + 저장 미호출) + NotFound.
- 테스트 이름은 백틱 한글 "조건이면 결과".

```bash
./gradlew :server:test --tests "*EventServiceTest"
```
