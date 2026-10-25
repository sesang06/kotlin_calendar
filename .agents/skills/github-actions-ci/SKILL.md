---
name: github-actions-ci
description: kotlin_calendar의 GitHub Actions CI 규칙. .github/workflows/ci.yml의 잡 구성(ktlint, 서버 테스트, 클라이언트 JVM/Android/Wasm 테스트, iOS 테스트), 새 모듈·테스트 태스크를 CI에 추가하는 방법, 캐시·시크릿·실패 리포트 처리가 필요할 때 사용. CI 워크플로를 만들거나 고치거나 CI 실패를 조사할 때 사용.
---

# GitHub Actions CI

## 워크플로

`.github/workflows/ci.yml` — `main` push와 모든 PR에서 실행. 같은 ref의 이전 실행은 취소(`concurrency`).

| 잡 | 러너 | 실행 | 비고 |
|---|---|---|---|
| `lint` | ubuntu | `./gradlew ktlintCheck --continue` | `ktlint` 스킬 |
| `server-test` | ubuntu | `./gradlew :server:test` | H2 + MockK. Supabase 접속 안 함 |
| `migration-test` | ubuntu + 서비스 컨테이너 `postgres:16` | `./gradlew :server:test --tests "*MigrationIntegrationTest"` | `MIGRATION_TEST_POSTGRES_URL`로 실제 DB에 Flyway 적용 (`server-database`) |
| `client-test` | ubuntu | `:client:shared:desktopTest`, `testDebugUnitTest`, `wasmJsBrowserTest`(헤드리스 Chrome — skiko Wasm은 Node 미지원), desktop 컴파일, Android `assembleDebug`, web `wasmJsBrowserDistribution` | Android SDK는 러너에 기본 설치 |
| `client-ios-test` | macos | `:client:shared:iosSimulatorArm64Test` | iOS는 macOS에서만 빌드 가능 |

공통 셋업: `actions/checkout@v4` → `actions/setup-java@v4` (temurin 21) → `gradle/actions/setup-gradle@v4` (Gradle 캐시 자동).

## 규칙

1. **로컬에서 같은 명령이 통과해야** 한다. CI 전용 스크립트를 만들지 않고 Gradle 태스크만 호출한다.
2. 테스트는 외부 서비스(Supabase 등)에 의존하지 않는다. 필요하면 H2·MockK·MockEngine으로 대체.
3. 시크릿이 필요한 잡(배포 등)은 별도 워크플로로 분리하고 `pull_request`에서 돌리지 않는다. 시크릿은 `${{ secrets.XXX }}`로만 참조.
4. 실패 시 테스트 리포트를 `actions/upload-artifact@v4`로 올린다 (`build/reports/tests`).
5. `permissions: contents: read` 최소 권한 유지.
6. 액션 버전은 메이저 태그(`@v4`)로 고정.

## 새 모듈/테스트를 추가할 때

- 새 Gradle 모듈 → 해당 잡의 `./gradlew` 목록에 테스트 태스크 추가 (예: `:client:newModule:desktopTest`).
- 새 플랫폼 타깃 → 해당 러너가 필요한지 확인 (Apple 타깃 = macos).
- 무거운 잡은 `needs: lint`로 lint 이후에 돌리는 것을 고려.

## CI 실패 조사

```bash
gh run list --limit 5
gh run view <run-id> --log-failed
gh run download <run-id> -n server-test-reports
```

로컬 재현: 실패한 잡의 `./gradlew ...` 명령을 그대로 실행.
