#!/bin/bash
set -euo pipefail

echo "=== Vercel Build Script for Kotlin Calendar ==="

# 프로젝트 toolchain(jvmToolchain(21))·CI와 같은 JDK 21 이상을 쓴다.
# Vercel 빌드 이미지(Amazon Linux 2023)에는 Corretto 11이 기본으로 있으므로 버전까지 확인한다.
REQUIRED_JAVA_MAJOR=21

java_major() {
    command -v java &> /dev/null || { echo 0; return; }
    java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.specification.version/ {print $2}' | cut -d. -f1
}

if [ "$(java_major)" -lt "$REQUIRED_JAVA_MAJOR" ]; then
    echo "JDK ${REQUIRED_JAVA_MAJOR}+ not found. Installing Amazon Corretto ${REQUIRED_JAVA_MAJOR}..."
    if command -v dnf &> /dev/null; then
        dnf install -y "java-${REQUIRED_JAVA_MAJOR}-amazon-corretto-devel"
        export JAVA_HOME="/usr/lib/jvm/java-${REQUIRED_JAVA_MAJOR}-amazon-corretto"
    elif command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y "openjdk-${REQUIRED_JAVA_MAJOR}-jdk-headless"
        export JAVA_HOME="/usr/lib/jvm/java-${REQUIRED_JAVA_MAJOR}-openjdk-amd64"
    fi
    export PATH="${JAVA_HOME:-}/bin:$PATH"
fi

if [ "$(java_major)" -lt "$REQUIRED_JAVA_MAJOR" ]; then
    echo "ERROR: JDK ${REQUIRED_JAVA_MAJOR}+ could not be installed." >&2
    exit 1
fi

echo "Java version:"
java -version

chmod +x ./gradlew

echo "Building client webApp (Wasm)..."
./gradlew :client:webApp:wasmJsBrowserDistribution --no-daemon --stacktrace

echo "Build complete. Distribution files located at client/webApp/build/dist/wasmJs/productionExecutable"
