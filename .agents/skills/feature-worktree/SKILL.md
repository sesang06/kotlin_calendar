---
name: feature-worktree
description: kotlin_calendar 작업을 피처 단위로 나누고 피처마다 git worktree와 브랜치를 만들어 병렬로 개발하는 규칙. 요청을 피처로 쪼개는 기준, 브랜치·worktree 이름, 의존 피처(stacked) 처리, worktree별 Gradle 설정, 머지 후 정리가 필요할 때 사용. 코드 변경이 들어가는 작업을 시작할 때 가장 먼저 읽는다.
---

# 피처별 worktree

**한 피처 = 한 브랜치 = 한 worktree = 한 PR.** `main`에서 직접 작업하지 않는다.

## 1. 작업을 피처로 나누기

요청을 받으면 코드를 쓰기 전에 피처 목록을 만든다.

| 기준 | 예 |
|---|---|
| 혼자 머지돼도 빌드·테스트가 통과한다 | `server-event-crud`는 클라이언트 없이도 머지 가능 |
| 리뷰어가 한 번에 이해할 수 있다 (변경 ~400줄 이하 권장) | 크면 계층별로 쪼갬: `server-event-repository` → `server-event-service` → `server-event-handlers` |
| 영역이 섞이지 않는다 | 서버·클라이언트·proto·CI 변경은 가능하면 다른 피처 |

순서 규칙:
1. **proto 변경은 별도 피처로 먼저** (`proto-<리소스>`). 서버·클라이언트 피처가 그 위에 쌓인다.
2. 서로 의존하지 않는 피처는 병렬로 진행한다 (각자 `main`에서 분기).
3. 의존하는 피처는 **stacked**: 선행 피처 브랜치에서 분기하고, PR base도 선행 브랜치로 둔다.

피처 목록은 이 형식으로 사용자에게 먼저 보여준다:

```
| # | 피처(브랜치) | 범위 | base | 의존 |
|---|---|---|---|---|
| 1 | feat/proto-event | proto/calendar/v1/event.proto | main | - |
| 2 | feat/server-event-crud | server/** | feat/proto-event | 1 |
| 3 | feat/client-calendar-screen | client/shared/** | feat/proto-event | 1 |
| 4 | chore/ci-cache | .github/** | main | - |
```

## 2. 이름 규칙

| 항목 | 규칙 | 예 |
|---|---|---|
| 브랜치 | `<type>/<scope>-<요약>` (kebab-case). type: `feat`, `fix`, `refactor`, `test`, `chore`, `docs` | `feat/server-event-crud` |
| worktree 경로 | 저장소 **바깥** 형제 디렉터리 `../kotlin_calendar-worktrees/<브랜치의 / 를 - 로>` | `../kotlin_calendar-worktrees/feat-server-event-crud` |
| scope | `proto`, `server`, `client`, `ci`, `build`, `agents` | |

worktree를 저장소 안에 만들지 않는다 (Gradle·ktlint가 중첩 프로젝트로 인식).

## 3. worktree 만들기

```bash
git fetch origin
# 독립 피처
git worktree add -b feat/server-event-crud ../kotlin_calendar-worktrees/feat-server-event-crud origin/main
# stacked 피처
git worktree add -b feat/client-calendar-screen ../kotlin_calendar-worktrees/feat-client-calendar-screen feat/proto-event

# 커밋되지 않는 로컬 설정 복사
cp local.properties ../kotlin_calendar-worktrees/feat-server-event-crud/
```

- 원격이 아직 없으면 `origin/main` 대신 `main`.
- 이후 그 피처의 모든 편집·빌드·커밋은 **그 worktree 디렉터리 안에서만** 한다. 다른 worktree의 파일을 건드리지 않는다.
- 각 worktree는 자기 `build/`를 쓴다. 첫 빌드는 느리지만 Gradle 캐시(`~/.gradle`)는 공유된다.
- 여러 피처를 병렬 에이전트로 돌릴 때는 에이전트 하나에 worktree 하나만 맡긴다.

## 4. 병렬 실행 (필수)

worktree를 나누는 이유는 **동시에 진행하기 위해서**다. 독립 피처를 한 에이전트가 하나씩 차례로 처리하지 않는다.

1. 피처 목록을 보여준 직후, **선행 피처가 없는 모든 피처**에 대해 worktree를 만들고 피처마다 백그라운드 에이전트를 하나씩 동시에 띄운다.
2. 에이전트에게 넘기는 것: worktree 절대 경로, 브랜치, 할 일, 읽을 스킬, **건드리면 안 되는 경로**(다른 에이전트가 같은 파일을 고치는 경우), 검증 명령, 커밋 규칙, push·PR 여부.
3. 조율하는 쪽(메인 에이전트)은 직접 구현을 붙잡고 기다리지 않는다. 결과가 오는 대로 다음 단계를 병렬로 이어간다:
   - 피처 완료 → 즉시 push·PR → 리뷰 에이전트들을 병렬로 띄움
   - 그 PR의 리뷰어가 모두 결과를 올리면 → 작성 에이전트(해당 worktree)가 반영 (`pull-request` 3·4단계). PR끼리는 서로 기다리지 않는다.
   - stacked 피처는 선행 피처의 **첫 커밋이 나오자마자** 그 위에서 시작 (선행 PR 머지를 기다리지 않음). 선행 브랜치는 다른 에이전트가 계속 바꾸므로 후속 에이전트는 6단계로 갱신을 받아 오고, 후속 PR은 선행 브랜치가 `origin`에 push된 뒤에 연다.
4. 같은 파일을 여러 피처가 고쳐야 하면 한 피처만 그 파일을 맡게 범위를 조정하고, 나머지에는 "이 파일 수정 금지"를 명시한다.
5. 병렬로 돌릴 수 없는 이유가 있으면(같은 파일 대량 수정, 생성 코드 의존 등) 사용자에게 그 이유를 말한다.

## 5. 피처 안에서의 작업

1. 관련 스킬을 읽는다 (`AGENTS.md`의 스킬 표).
2. 작은 단위로 커밋 (Conventional Commits: `feat(server): add EventService.createEvent`).
3. 끝나면 `pull-request` 스킬로 검증 → push → PR.
4. 리뷰 반영도 같은 worktree에서 커밋을 추가한다 (force-push로 리뷰 이력을 지우지 않는다).

## 6. stacked 피처 갱신

선행 피처가 수정되면:

```bash
cd ../kotlin_calendar-worktrees/feat-client-calendar-screen
git fetch origin
git rebase origin/feat/proto-event     # 아직 push 전이면 rebase
git merge origin/feat/proto-event      # 한 번이라도 push했으면 merge (force-push 금지)
```

선행 PR이 `main`에 머지되면 후속 PR의 base를 `main`으로 바꾼다: `gh pr edit <번호> --base main`. "Projects (classic)" GraphQL 오류가 나면 `gh api -X PATCH repos/{owner}/{repo}/pulls/<번호> -f base=main`.

## 7. 정리

PR이 머지되면:

```bash
git worktree remove ../kotlin_calendar-worktrees/feat-server-event-crud
git branch -d feat/server-event-crud
git worktree prune
```

`git worktree list`로 남은 worktree를 확인한다. 머지되지 않은 worktree는 사용자 확인 없이 지우지 않는다.
