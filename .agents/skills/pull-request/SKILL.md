---
name: pull-request
description: kotlin_calendar 피처 브랜치를 GitHub Pull Request로 올리는 규칙. PR 전 로컬 검증(ktlint·테스트), push, gh pr create 본문 형식, stacked PR base 설정, (사용자가 요청할 때) 리뷰 에이전트 요청, 리뷰 반영과 머지 조건이 필요할 때 사용. feature-worktree에서 피처 구현을 마친 뒤 사용.
---

# Pull Request

## 사전 조건

- `gh auth status`가 로그인 상태이고 `origin` 원격이 있어야 한다. 없으면 사용자에게 저장소 생성/연결을 요청한다 (`gh repo create` 등은 사용자 확인 후).
- 피처 worktree 안에서 실행한다 (`feature-worktree`).

## 1. 올리기 전 검증 (worktree 안에서)

변경 범위에 맞춰 실행하고 **전부 통과해야 PR을 연다.**

| 변경 경로 | 명령 |
|---|---|
| 항상 | `./gradlew ktlintCheck` |
| `proto/**` | `./gradlew :server:generateProto :client:shared:generateCommonMainProtos` |
| `server/**` | `./gradlew :server:test` |
| `client/shared/**` | `./gradlew :client:shared:desktopTest :client:shared:testDebugUnitTest` |
| `client/*App/**` | 해당 앱 컴파일 (`:client:desktopApp:compileKotlin` 등) |
| `.github/**` | YAML 문법 확인, 바뀐 잡의 Gradle 명령을 로컬에서 실행 |

실패하면 고치고 다시 검증한다. 검증하지 못한 항목(예: iOS)은 PR 본문에 적는다.

## 2. push와 PR 생성

```bash
git push -u origin feat/server-event-crud

gh pr create \
  --base main \                       # stacked면 선행 브랜치 (예: feat/proto-event)
  --head feat/server-event-crud \
  --title "feat(server): 일정 CRUD API (HTTP·gRPC)" \
  --body-file <본문 파일>
```

- 제목: Conventional Commits 형식 `<type>(<scope>): <요약>`.
- 본문은 `.github/pull_request_template.md` 형식을 채운다. 비워 둔 섹션을 남기지 않는다.
- 피처 하나에 PR 하나. 관련 없는 변경이 섞였으면 PR을 열기 전에 브랜치를 나눈다.
- stacked PR은 본문 "의존" 항목에 선행 PR 링크를 적는다.
- 드래프트가 필요하면 `--draft`, 리뷰 준비가 되면 `gh pr ready`.

## 3. 리뷰 요청 (선택 — 사용자가 요청할 때만)

리뷰 에이전트는 **기본으로 띄우지 않는다.** 사용자가 리뷰를 요청했을 때만 아래를 따른다. 요청이 없으면 PR 링크를 전하고 "리뷰 에이전트가 필요하면 요청해 달라"고 한 줄로 알린 뒤 멈춘다.

요청을 받으면 PR을 연 **작성 에이전트는 자기 PR을 리뷰하지 않는다.** PR마다 별도 리뷰 에이전트를 띄워 `pr-review` 스킬로 리뷰하게 한다.

1. `gh pr diff <번호> --name-only`로 바뀐 경로를 보고 `pr-review`의 "리뷰어 배정 표"로 필요한 리뷰어 역할을 정한다.
2. 역할마다 **새 컨텍스트의 에이전트**(작성 대화를 물려받지 않음)를 띄운다. 넘기는 정보는 PR 번호, 역할, `pr-review` 스킬 경로뿐이다. 구현 의도는 PR 본문으로만 전달한다.
3. 리뷰어들은 서로 독립적으로 병렬 실행한다.
4. 모든 리뷰어의 결과(`gh pr review --comment`)가 달리면 작성 에이전트가 반영한다.

## 4. 리뷰 반영

- 🔴 Blocker / 🟠 Major는 반드시 고치거나, 고치지 않는 이유를 해당 코멘트에 답한다.
- 🟡 Minor / 🔵 Nit는 판단해서 반영. 반영하지 않으면 한 줄로 이유를 남긴다.
- 반영은 같은 worktree에서 **새 커밋**으로 (`fix(server): address review — ...`). 리뷰 후 force-push 금지.
- 반영 후 1단계 검증을 다시 돌리고, 리뷰 에이전트를 띄운 경우 Blocker/Major를 낸 리뷰어 역할만 다시 리뷰를 요청한다.
- 여러 PR의 리뷰 반영은 PR(=worktree)마다 작성 에이전트를 따로 띄워 병렬로 한다 (`feature-worktree`의 "병렬 실행").
- stacked PR의 선행 브랜치가 바뀌면 후속 브랜치에는 **merge**로 반영한다 (이미 push했으면 rebase + force-push 금지).

## 5. 머지 조건

- CI(`ci.yml`) 전부 통과
- 리뷰를 받은 경우: 모든 리뷰어 결과가 `APPROVE` 또는 남은 지적이 Minor/Nit뿐 (리뷰 에이전트를 요청하지 않았으면 이 조건은 없다)
- stacked PR은 선행 PR이 먼저 머지되고 base가 `main`으로 바뀐 뒤
- **머지는 사용자가 하거나 사용자가 명시적으로 지시했을 때만.** 기본 방식은 squash merge.

머지 후 `feature-worktree`의 "정리" 단계를 따른다.

## 자주 겪는 문제

| 증상 | 대응 |
|---|---|
| `./gradlew … \| grep …` 결과가 성공처럼 보임 | 파이프의 종료 코드는 grep 것이다. `$?`를 직접 보거나 `server/build/test-results/test/*.xml`의 `failures`를 확인 |
| `gh pr edit` 가 "Projects (classic)" GraphQL 오류로 실패 | `gh api -X PATCH repos/{owner}/{repo}/pulls/<n> -F body=@<file>` 로 본문 수정 |
| Windows에서 Python으로 YAML 검사 시 `cp949` 디코드 오류 | `open(path, encoding="utf-8")` |
| 앞 명령이 실패했는데 `&&` 뒤 커밋이 빠진 채 push됨 | push 전에 `git log --oneline -3`으로 커밋이 있는지 확인 |
