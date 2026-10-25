---
name: workflow
description: 코드 변경 작업의 흐름. 피처 분할, 브랜치·worktree, 병렬 에이전트, stacked 피처, PR 전 검증, PR 생성, 리뷰 반영, 머지·정리. 코드가 바뀌는 작업을 시작할 때 먼저 읽는다.
---

# 작업 흐름

**한 피처 = 한 브랜치 = 한 worktree = 한 PR.** `main`에 직접 커밋하지 않는다.

## 1. 피처 분할

코드 작성 전에 요청을 독립적으로 머지 가능한 피처로 나누고 표로 보여준다.

```
| # | 브랜치 | 범위 | base | 의존 |
| 1 | feat/proto-event | proto/** | main | - |
| 2 | feat/server-event-crud | server/** | feat/proto-event | 1 |
```

- 혼자 머지돼도 빌드·테스트 통과, 변경 ~400줄 이하, 서버·클라이언트·proto·CI는 가능하면 다른 피처.
- proto 변경은 별도 피처로 먼저. 의존 피처는 stacked(선행 브랜치에서 분기, PR base도 선행 브랜치).
- 브랜치: `<type>/<scope>-<요약>` (type: feat/fix/refactor/test/chore/docs, scope: proto/server/client/ci/build/agents).

## 2. worktree + 병렬 실행

```bash
git fetch origin
git worktree add -b feat/x ../kotlin_calendar-worktrees/feat-x origin/main   # stacked면 선행 브랜치에서
cp local.properties ../kotlin_calendar-worktrees/feat-x/ 2>/dev/null
```

- worktree는 저장소 **바깥**에 만든다. 편집·빌드·커밋은 그 worktree 안에서만.
- 피처가 1~2개이거나 작으면 에이전트를 띄우지 않고 직접 한다 (서브에이전트는 컨텍스트를 처음부터 다시 읽어 토큰이 많이 든다).
- 독립 피처가 3개 이상이고 각각 클 때만 **피처마다 에이전트 하나씩 동시에** 띄운다. 넘길 것: worktree 경로, 브랜치, 할 일, 읽을 스킬, 건드리면 안 되는 경로, 검증 명령.
- stacked 피처는 선행 피처 첫 커밋이 나오면 바로 시작. 선행이 바뀌면 push 전엔 rebase, push 후엔 merge (force-push 금지).

## 3. 검증 → PR

전부 통과해야 PR을 연다.

| 변경 | 명령 |
|---|---|
| 항상 | `./gradlew ktlintFormat ktlintCheck` |
| `proto/**` | `./gradlew :server:generateProto :client:shared:generateCommonMainProtos` |
| `server/**` | `./gradlew :server:test` |
| `client/shared/**` | `./gradlew :client:shared:desktopTest :client:shared:testDebugUnitTest` |
| `client/*App/**` | 해당 앱 컴파일 (`:client:desktopApp:compileKotlin` 등) |

- 커밋·PR 제목: Conventional Commits `<type>(<scope>): <요약>`.
- PR 본문은 `.github/pull_request_template.md`를 채운다. 검증 못 한 항목(iOS 등)은 적는다. stacked면 선행 PR 링크.
- `gh pr create --base <main|선행 브랜치> --head <브랜치> --title ... --body-file ...`
- `./gradlew … | grep`은 종료 코드를 가린다. push 전 `git log --oneline -3`으로 커밋 확인.

## 4. 리뷰 (선택)

리뷰 에이전트는 **사용자가 요청했을 때만** 띄운다(`pr-review`). 요청이 없으면 PR 링크와 "리뷰가 필요하면 요청해 달라"는 한 줄만 남기고 멈춘다.

리뷰 지적 반영: Blocker/Major는 고치거나 이유를 답한다. 반영은 같은 worktree에서 새 커밋으로 (리뷰 후 force-push 금지).

## 5. 머지·정리

- 조건: CI 통과, (리뷰를 받았다면) Blocker/Major 없음, stacked는 선행 PR 머지 후 base를 `main`으로 (`gh pr edit <n> --base main`).
- **머지는 사용자가 하거나 명시적으로 지시할 때만.** 기본 squash.
- 머지 후 `git worktree remove <경로> && git branch -d <브랜치>`. 머지 안 된 worktree는 사용자 확인 없이 지우지 않는다.
