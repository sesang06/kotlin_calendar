---
name: ktlint
description: kotlin_calendar의 Kotlin 코드 스타일·린트 규칙(ktlint). ktlint Gradle 플러그인 설정, .editorconfig 규칙, ktlintCheck/ktlintFormat 실행, 자주 걸리는 규칙(와일드카드 import, trailing comma, Composable 함수 이름) 처리, 생성 코드 제외가 필요할 때 사용. Kotlin 파일을 작성·수정한 뒤나 CI의 lint 잡이 실패했을 때 사용.
---

# ktlint

## 설정 위치

| 파일 | 내용 |
|---|---|
| `build.gradle.kts` (루트) | `org.jlleitschuh.gradle.ktlint` 플러그인을 `allprojects`에 적용, `build/` 아래(생성 코드) 제외 |
| `.editorconfig` | `ktlint_code_style = ktlint_official`, `max_line_length = 140`, `@Composable` 함수 이름 예외 |
| `gradle/libs.versions.toml` | `ktlint-plugin` 버전 |

규칙을 바꿀 때는 **`.editorconfig`만** 고친다. 코드에 `@Suppress("ktlint:...")`는 정말 예외적인 경우에만, 이유 주석과 함께.

## 명령

```bash
./gradlew ktlintCheck            # 전체 검사 (CI와 동일)
./gradlew ktlintFormat           # 자동 수정
./gradlew :server:ktlintCheck    # 모듈 하나만
```

Kotlin 파일을 만들거나 고친 뒤에는 **`ktlintFormat` → `ktlintCheck`** 순서로 돌리고, 자동 수정이 안 된 항목만 손으로 고친다.

## 자주 걸리는 규칙

| 규칙 | 대응 |
|---|---|
| `no-wildcard-imports` | `import a.b.*` 금지. 하나씩 import (자동 수정 안 됨 → 직접 작성) |
| `trailing-comma-on-declaration-site` / `call-site` | 여러 줄 파라미터·인자 끝에 `,` (ktlintFormat이 고침) |
| `function-naming` | `@Composable`은 PascalCase 허용(.editorconfig). 테스트는 백틱 한글 이름 허용 |
| `property-naming` | `const val`·top-level 상수는 `UPPER_SNAKE_CASE`. 단 `val ProtobufContentType = ContentType(...)`처럼 객체는 PascalCase 허용 |
| `max-line-length` (140) | 체인 호출은 줄바꿈 |
| `no-unused-imports` | 사용 안 하는 import 제거 |
| `filename` | 파일에 top-level 클래스 하나면 파일명 = 클래스명 |
| `multiline-expression-wrapping` | `val x =` 다음 여러 줄 식은 줄바꿈 후 들여쓰기 (ktlintFormat이 고침) |

## 제외 대상

- protoc·Wire 생성 코드(`build/generated/**`)는 루트 설정에서 이미 제외. 생성 코드를 `src/`로 복사하지 않는다.
- `*.gradle.kts`도 검사 대상이다.

## CI

`.github/workflows/ci.yml`의 `lint` 잡이 `./gradlew ktlintCheck --continue`를 실행한다. 실패하면 로컬에서 `ktlintFormat` 후 다시 커밋.
