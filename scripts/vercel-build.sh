#!/bin/bash
set -euo pipefail

echo "=== Vercel Build Script for Kotlin Calendar ==="

# 프로젝트 toolchain(jvmToolchain(21))·CI와 같은 JDK 21을 쓴다. Vercel 빌드 이미지는 Amazon Linux 2023(dnf).
if ! command -v java &> /dev/null; then
    echo "Java not found. Installing Amazon Corretto 21..."
    if command -v dnf &> /dev/null; then
        dnf install -y java-21-amazon-corretto-devel
    elif command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y openjdk-21-jdk-headless
    fi
fi

if ! command -v java &> /dev/null; then
    echo "ERROR: JDK 21 could not be installed." >&2
    exit 1
fi

echo "Java version:"
java -version

chmod +x ./gradlew

echo "Building client webApp (Wasm)..."
./gradlew :client:webApp:wasmJsBrowserDistribution --no-daemon --stacktrace

echo "Build complete. Distribution files located at client/webApp/build/dist/wasmJs/productionExecutable"
