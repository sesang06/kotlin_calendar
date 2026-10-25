#!/bin/bash
# Vercel "Ignored Build Step": exit 0 = 빌드 건너뜀, exit 1 = 빌드 진행.
# main에 머지(push)된 커밋만 배포하고, PR·다른 브랜치의 프리뷰 배포는 건너뛴다.
# Git 연동이 아닌 배포(vercel CLI 등)는 VERCEL_GIT_COMMIT_REF가 비어 있으므로 그대로 빌드한다.
set -u

PRODUCTION_BRANCH="main"
REF="${VERCEL_GIT_COMMIT_REF:-}"

if [ -z "$REF" ] || [ "$REF" = "$PRODUCTION_BRANCH" ]; then
    echo "Build: ref='${REF:-<none>}'"
    exit 1
fi

echo "Skip: '$REF' is not $PRODUCTION_BRANCH (deploy on merge only)"
exit 0
