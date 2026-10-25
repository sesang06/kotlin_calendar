---
name: pr-review
description: 사용자가 PR 리뷰를 요청했을 때만 사용. 리뷰어 역할 배정, 역할별 체크포인트, 심각도, gh 리뷰 코멘트 형식.
---

# PR 리뷰 (사용자 요청 시에만)

PR 작성 에이전트는 리뷰하지 않는다. 역할마다 **새 컨텍스트 에이전트**를 띄우고 PR 번호·역할·이 스킬 경로만 넘긴다. 리뷰어는 PR 본문·diff·스킬만 근거로 판단하고 **코드를 고치거나 push하지 않는다.**

## 역할 배정 (`gh pr diff <n> --name-only`)

| 역할 | 배정 경로 | 기준 스킬 | 볼 것 |
|---|---|---|---|
| architecture | `server/**/main/**`, `client/**/commonMain/**`, `client/*App/**` | `server`, `client` | 계층 방향, 패키지 위치, DI, MVVM, proto가 data 밖으로 새지 않음 |
| testing | `server/**`, `client/shared/**` | `server-testing`, `client-testing` | 바뀐 코드마다 테스트, 실패 경로, `coEvery`, 고정 시간·ID, 외부 접속 없음 |
| contract | `proto/**`, `**/handler/**`, `**/data/remote/**` | `proto-api` | 필드 번호·`reserved`, 네이밍, HTTP 매핑, 서버·클라이언트 양쪽 반영, 에러 매핑 |
| security-config | `**/application.yaml`, `**/database/**`, `**/db/migration/**`, `.github/**`, `gradle/**`, `*.gradle.kts` | `server-database`, `ci-lint` | 비밀값 커밋, 머지된 V 파일 수정, RLS 누락, 해시 저장, CORS, CI 권한, 버전 고정 |

해당 없으면 architecture 하나. 자기 역할 범위만 보고, 범위 밖은 "범위 밖"으로 짧게.

## 심각도

🔴 Blocker(버그·보안·호환 깨짐·빌드 실패, 머지 막음) / 🟠 Major(스킬 규칙 위반·테스트 누락, 머지 막음) / 🟡 Minor / 🔵 Nit. 확인 안 한 추측은 Blocker로 올리지 않는다.

## 남기기

같은 계정이라 approve가 막히므로 항상 `gh pr review <n> --comment --body-file review.md`.

```markdown
**[역할] 판정: APPROVE | REQUEST_CHANGES**
요약: 한두 문장.
| 심각도 | 위치 | 내용 | 근거(스킬/실패 시나리오) |
확인하지 못한 것: …
```

Blocker/Major가 있으면 REQUEST_CHANGES. 재리뷰는 이전 지적 해결 여부만 본다.
