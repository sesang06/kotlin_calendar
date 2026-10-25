# Vercel 배포 가이드

`kotlin_calendar` 프로젝트의 웹 프론트엔드(Compose Multiplatform Wasm) 및 백엔드 라우팅 설정을 Vercel에 배포하는 방법입니다.

## 아키텍처 개요

- **Web Frontend**: Kotlin Multiplatform / Compose Multiplatform Wasm (`client/webApp`)
  - 빌드 결과물: `client/webApp/build/dist/wasmJs/productionExecutable`
  - WASM 실행에 필요한 COOP / COEP 헤더 및 MIME Type 설정 (`vercel.json`)
- **Server / API Gateway**: Vercel Serverless API Proxy (`api/proxy.js`)
  - `/api/*` 요청을 백엔드 Ktor 서버(`BACKEND_API_URL` 환경변수)로 전달
  - 백엔드 URL 미설정 시에도 게이트웨이 헬스체크 응답 반환

## 환경 변수 설정 (Vercel Project Settings)

- `BACKEND_API_URL` (선택 사항): Ktor 서버가 호스팅된 URL (예: `https://api.yourcalendar.com` 또는 Cloud Run / Render / Supabase Edge URL)

## 배포 단계

1. **Vercel CLI 또는 웹 콘솔에서 프로젝트 임포트**
   - Framework Preset: `Other`
   - Build Command: `bash scripts/vercel-build.sh` (또는 `vercel.json`에 정의된 명령 자동 감지)
   - Output Directory: `client/webApp/build/dist/wasmJs/productionExecutable`
   - Root Directory: `./` (프로젝트 루트)

2. **빌드 실행**
   - Vercel의 빌드 환경에서 Java 17 및 Gradle을 통해 Wasm 브라우저 번들을 빌드하고 정적 자산과 서버리스 함수를 배포합니다.
