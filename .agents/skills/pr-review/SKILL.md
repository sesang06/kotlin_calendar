---
name: pr-review
description: kotlin_calendar Pull Request를 별도의 리뷰 에이전트가 리뷰하는 규칙. 리뷰어 역할(architecture, testing, contract, security-config)과 변경 경로별 배정, 역할별 체크리스트, 심각도 기준, gh로 리뷰 코멘트를 남기는 형식이 필요할 때 사용. PR 리뷰를 맡은 에이전트이거나 PR 리뷰를 요청받았을 때 사용.
---

# PR 리뷰 (리뷰 에이전트용)

너는 이 PR을 **작성하지 않은 독립 리뷰어**다. 작성자의 대화 기록 없이 PR 본문, diff, 저장소의 스킬만 근거로 판단한다.

## 입력

- PR 번호, 맡은 **역할** (아래 표), 이 스킬

## 리뷰어 역할과 배정

| 역할 | 보는 것 | 배정 조건 (`gh pr diff <n> --name-only`) | 함께 읽을 스킬 |
|---|---|---|---|
| `architecture` | 계층·의존 방향, 패키지 위치, DI 배선, MVVM 규칙, 중복 | `server/**/main/**`, `client/**/commonMain/**`, `client/*App/**` | `ktor-server-structure`, `ktor-handler`, `ktor-service`, `ktor-repository`, `kmp-mvvm`, `kmp-data-layer`, `kmp-project-structure` |
| `testing` | 테스트 존재·충분성, MockK·Fake 사용법, 경계값, 외부 의존 없음 | `server/**`, `client/shared/**` (main 코드가 바뀌면 항상) | `ktor-mockk-testing`, `kmp-testing` |
| `contract` | proto 하위 호환, 네이밍, HTTP 매핑 표, 서버·클라이언트 양쪽 반영 | `proto/**`, `**/handler/**`, `**/data/remote/**` | `proto-contract`, `api-transport` |
| `security-config` | 비밀값·접속 정보 노출, DB 설정·주입, CORS, CI 권한, 의존성 버전 | `**/application.yaml`, `**/database/**`, `.github/**`, `gradle/**`, `*.gradle.kts` | `server-database`, `github-actions-ci`, `ktlint` |

- 조건에 하나라도 걸리면 그 역할을 배정한다. 아무 것도 안 걸리면(문서만 바뀐 PR 등) `architecture` 하나만.
- **자기 역할 범위만** 리뷰한다. 범위 밖 문제를 우연히 봤으면 "범위 밖" 표시로 짧게만 남긴다.

## 절차

1. 맥락 파악
   ```bash
   gh pr view <n> --json title,body,baseRefName,headRefName,files
   gh pr diff <n>
   gh pr checks <n>
   ```
2. 역할의 "함께 읽을 스킬"을 읽는다. **스킬 규칙이 리뷰 기준**이다. 개인 취향은 Nit로만.
3. 필요하면 PR 브랜치를 읽기 전용으로 확인한다 (`gh pr checkout <n>`은 별도 worktree에서). **코드를 고치거나 커밋·push하지 않는다.**
4. 역할 체크리스트를 확인하고 지적마다 파일·줄과 근거(스킬 이름 또는 구체적 실패 시나리오)를 적는다.
5. 아래 형식으로 리뷰를 단다.

## 역할별 체크리스트

**architecture**
- Handler → Service → Repository 단방향인가? Service가 `ApplicationCall`·proto 클래스를 import하지 않는가?
- HTTP·gRPC Handler가 같은 Service·매퍼를 쓰는가?
- Repository가 벤더 중립 Exposed DSL만 쓰고 `Database`를 주입받는가?
- ViewModel이 Compose·플랫폼 API를 import하지 않는가? UiState 불변, Effect는 Channel인가?
- proto 메시지가 data 계층 밖으로 새지 않는가?
- 새 의존성이 버전 카탈로그에 있는가?

**testing**
- 바뀐 main 코드마다 대응 테스트가 있는가? (Service → MockK, HTTP·gRPC Handler → 둘 다, Repository → H2, ViewModel → Fake + Turbine)
- 실패 경로가 테스트되는가? (예외 타입 + 저장 미호출 `coVerify(exactly = 0)`)
- suspend 스텁에 `coEvery`를 쓰는가? strict mock인가?
- 시간·ID가 고정값으로 주입되는가? 외부 네트워크·Supabase에 접속하지 않는가?
- `gh pr checks` 결과와 PR 본문의 검증 항목이 일치하는가?

**contract**
- 필드 번호 재사용·타입 변경·삭제 시 `reserved` 누락이 없는가?
- 네이밍(snake_case 필드, `XxxApi` 서비스, Request/Response 쌍), 시각 = epoch millis 규칙
- 새 RPC가 HTTP 매핑 표와 서버 HTTP·gRPC Handler, 클라이언트 RemoteDataSource 양쪽에 반영됐는가?
- 에러 매핑(400/404/409 ↔ INVALID_ARGUMENT/NOT_FOUND/ALREADY_EXISTS)이 일관적인가?

**security-config**
- URL·비밀번호·토큰이 하드코딩되거나 커밋되지 않았는가? (`application.yaml`은 `$ENV:default`만)
- DB 선택이 `databaseModule` 한 곳에서만 이뤄지는가?
- CORS가 필요한 host만 허용하는가?
- CI `permissions` 최소, 시크릿이 `pull_request`에서 쓰이지 않는가?
- 의존성 버전이 고정돼 있고 알려진 문제 버전이 아닌가?

## 심각도

| 표시 | 의미 | 머지 |
|---|---|---|
| 🔴 Blocker | 버그·데이터 손상·보안 문제·하위 호환 깨짐·빌드/테스트 실패 | 막음 |
| 🟠 Major | 스킬 규칙 위반(계층, 테스트 누락 등), 유지보수에 실질적 문제 | 막음 |
| 🟡 Minor | 개선하면 좋은 점, 작은 규칙 위반 | 막지 않음 |
| 🔵 Nit | 취향·표현 | 막지 않음 |

추측이면 추측이라고 적는다. 확인하지 않은 것을 Blocker로 올리지 않는다.

## 리뷰 남기기

같은 GitHub 계정으로는 approve/request-changes가 막히므로 **항상 `--comment`**로 남기고 판정은 본문 첫 줄에 쓴다.

```bash
gh pr review <n> --comment --body-file review.md
```

`review.md` 형식:

```markdown
**[architecture] 판정: REQUEST_CHANGES**   <!-- APPROVE | REQUEST_CHANGES | COMMENT -->

요약: 한두 문장.

| 심각도 | 위치 | 내용 | 근거 |
|---|---|---|---|
| 🔴 Blocker | server/.../EventService.kt:42 | ... | ktor-service 규칙 1 |
| 🟡 Minor | ... | ... | ... |

확인한 것: 체크리스트 중 문제없던 항목 요약
확인하지 못한 것: (예: iOS 빌드)
```

- Blocker/Major가 하나라도 있으면 `REQUEST_CHANGES`, 없으면 `APPROVE`(Minor/Nit만 있으면 `APPROVE` + 목록).
- 줄 단위 지적이 꼭 필요하면 `gh api repos/{owner}/{repo}/pulls/<n>/comments`로 인라인 코멘트를 추가할 수 있다.
- 재리뷰 요청을 받으면 이전 지적이 해결됐는지만 확인하고 같은 형식으로 새 리뷰를 단다.
