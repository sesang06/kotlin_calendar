---
name: ci-lint
description: ktlint 규칙과 GitHub Actions CI. ktlintCheck/Format, 자주 걸리는 규칙, ci.yml 잡 구성, CI 실패 조사. 린트 오류나 CI 워크플로를 다룰 때 읽는다.
---

# ktlint + CI

## ktlint

- `ktlint_official`, `max_line_length = 140`, 설정은 `.editorconfig`만 고친다. `build/`(생성 코드)는 제외.
- Kotlin 수정 후 `./gradlew ktlintFormat ktlintCheck`. 자동 수정 안 되는 것만 손으로.
- 자주 걸림: 와일드카드 import 금지(직접 풀어 쓰기), 여러 줄 끝 trailing comma, 상수 `UPPER_SNAKE_CASE`, 미사용 import, 파일명 = top-level 클래스명. `@Composable`은 PascalCase 허용.
- `@Suppress("ktlint:…")`는 이유 주석과 함께 예외적으로만.

## CI (`.github/workflows/ci.yml`, main push·모든 PR)

| 잡 | 내용 |
|---|---|
| lint | `ktlintCheck --continue` |
| server-test | `:server:test` (H2 + MockK) |
| migration-test | postgres:16 서비스 컨테이너, `*MigrationIntegrationTest` |
| client-test | shared desktop/Android/wasmJs 테스트, desktop·Android·web 빌드 |
| client-ios-test | macOS, `iosSimulatorArm64Test` |

- CI는 Gradle 태스크만 호출한다 (로컬에서 같은 명령이 통과해야 함). 새 모듈·타깃은 해당 잡에 태스크 추가.
- `permissions: contents: read`, 액션은 메이저 태그 고정, 시크릿 쓰는 잡은 별도 워크플로로 분리하고 `pull_request`에서 돌리지 않는다.
- 실패 조사: `gh run view <id> --log-failed`, 로컬에서 같은 `./gradlew` 명령 재현.
