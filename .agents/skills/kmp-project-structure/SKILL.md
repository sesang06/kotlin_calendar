---
name: kmp-project-structure
description: kotlin_calendar 클라이언트의 Kotlin Multiplatform 모듈·타깃 구성 규칙. 웹(wasmJs)·앱(Android/iOS)·Windows 데스크톱(JVM) 타깃 설정, Gradle/Version Catalog, 플랫폼 진입점 모듈, expect/actual 배치, 소스셋 규칙이 필요할 때 사용. client/ 아래 build.gradle.kts나 *Main 소스셋을 만들거나 고칠 때 사용.
---

# KMP 프로젝트 구조

## 모듈과 타깃

| 모듈 | 타깃 | 역할 |
|---|---|---|
| `client/shared` | `androidTarget`, `iosArm64/iosSimulatorArm64`, `jvm("desktop")`, `wasmJs { browser() }` (Compose/skiko가 Node에서 못 돌아 `nodejs()` 없음) | 도메인·데이터·ViewModel·공통 Compose UI 전부 |
| `client/androidApp` | Android | `MainActivity`에서 `App()` 호출 |
| `client/iosApp` | iOS (Xcode) | `MainViewController()`를 SwiftUI에서 감쌈 |
| `client/desktopApp` | JVM | `application { Window { App() } }`, Windows 패키징 `TargetFormat.Msi`, `TargetFormat.Exe` |
| `client/webApp` | wasmJs (dev server 8081) | `ComposeViewport(document.body!!) { App() }` |

**진입점 모듈에는 로직을 두지 않는다.** `App()` 호출과 플랫폼 초기화(Koin 시작 등)만.

## 소스셋

```
client/shared/src/
├── commonMain/      # 거의 모든 코드
├── commonTest/
├── androidMain/     # actual 구현만
├── iosMain/
├── desktopMain/
└── wasmJsMain/
```

패키지 루트: `com.kotlincalendar.client`

## 기술 스택 (libs.versions.toml로 관리)

- `org.jetbrains.kotlin.multiplatform`, `org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`, `kotlinx-serialization`
- Compose: `compose.runtime`, `compose.foundation`, `compose.material3`, `compose.components.resources`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`
- `org.jetbrains.androidx.navigation:navigation-compose`
- `kotlinx-coroutines-core`, `kotlinx-datetime`, `kotlinx-serialization-json`(네비게이션 type-safe route용)
- 시간: `kotlin.time.Instant/Clock` — 모듈 전체에 `optIn.add("kotlin.time.ExperimentalTime")`
- Ktor Client (`kmp-data-layer` 참고), Koin
- proto: Wire 플러그인(`com.squareup.wire`) + `wire-runtime`, `wire-grpc-client` — 루트 `proto/`에서 commonMain 코드 생성 (`proto-contract`)

버전 문자열을 build.gradle.kts에 직접 쓰지 않는다. 항상 `libs.*` 참조.

## 플랫폼 차이 다루기

1. **우선순위**: 공통 인터페이스 + Koin `platformModule` 주입 > `expect/actual`.
2. `expect/actual`은 작은 것에만: HttpClient 엔진, `expect val platformModule: Module`, 로거.
3. 공통 코드 금지 사항:
   - `java.*`, `android.*` import
   - `runBlocking`, `Dispatchers.IO` (wasmJs 미지원) → `Dispatchers.Default`
   - 파일 시스템 직접 접근 (wasmJs 없음)

## Windows 데스크톱

```kotlin
compose.desktop {
    application {
        mainClass = "com.kotlincalendar.client.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "KotlinCalendar"
            windows { menu = true; shortcut = true }
        }
    }
}
```

- 실행: `./gradlew :client:desktopApp:run`
- 패키징: `./gradlew :client:desktopApp:packageMsi`

## 체크리스트

- 새 의존성 → `libs.versions.toml`에 추가 후 `commonMain.dependencies {}`에 우선 배치, 플랫폼 전용일 때만 `*Main`.
- 타깃 추가/변경 후 `./gradlew :client:shared:allTests`와 desktop 실행으로 확인.
