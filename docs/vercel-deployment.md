# Vercel 배포 가이드

`kotlin_calendar` 프로젝트의 웹 프론트엔드(Compose Multiplatform Wasm) 및 백엔드 라우팅 설정을 Vercel에 배포하는 방법입니다.

## 아키텍처 개요

- **Web Frontend**: Kotlin Multiplatform / Compose Multiplatform Wasm (`client/webApp`)
  - 빌드 결과물: `client/webApp/build/dist/wasmJs/productionExecutable`
  - WASM 실행에 필요한 COOP / COEP 헤더 및 MIME Type 설정 (`vercel.json`)
- **Server / API Gateway**: Vercel Serverless API Proxy (`api/proxy.js`)
  - `/api/*` 요청을 백엔드 Ktor 서버(`BACKEND_API_URL` 환경변수)로 경로 그대로 전달
  - protobuf 본문은 파싱하지 않고 원본 바이트로 전달, 요청 헤더는 허용 목록만 전달
  - 백엔드 origin 밖이나 `/api/` 밖 경로는 400, 백엔드 미설정·`https` 아님은 503
  - 웹 클라이언트는 배포 시 같은 origin(`window.location.origin`)으로, 로컬 dev server(:8081)에서는 `http://localhost:8080`으로 요청 (`PlatformModule.wasmJs.kt`)
  - 테스트: `node --test scripts/proxy.test.js`

## 환경 변수 설정 (Vercel Project Settings)

- `BACKEND_API_URL` (필수): Ktor 서버가 호스팅된 `https` URL (예: `https://api.yourcalendar.com`)

## 배포 단계

1. **Vercel CLI 또는 웹 콘솔에서 프로젝트 임포트**
   - Framework Preset: `Other`
   - Build Command: `bash scripts/vercel-build.sh` (또는 `vercel.json`에 정의된 명령 자동 감지)
   - Output Directory: `client/webApp/build/dist/wasmJs/productionExecutable`
   - Root Directory: `./` (프로젝트 루트)

2. **빌드 실행**
   - Vercel의 빌드 환경에서 JDK 21(기본 이미지의 Java 11처럼 21이 아니면 Amazon Corretto 21 설치, 실패 시 빌드 중단) 및 Gradle을 통해 Wasm 브라우저 번들을 빌드하고 정적 자산과 서버리스 함수를 배포합니다.

## 배포 시점

`main`에 머지된 커밋만 배포한다. PR·다른 브랜치 push는 프리뷰를 만들지 않는다.

- `vercel.json`의 `ignoreCommand`가 `scripts/vercel-ignore-build.sh`를 실행한다 (Vercel "Ignored Build Step": exit 0이면 건너뜀).
- `VERCEL_GIT_COMMIT_REF`가 `main`이 아니면 건너뛴다. Git 연동이 아닌 수동 배포(`vercel deploy`)는 그대로 빌드된다.
- 건너뛴 커밋은 Vercel·GitHub 상태에 "Canceled"(Ignored)로 표시된다.
- Vercel 대시보드 Settings → Git의 Production Branch가 `main`인지 확인한다.

