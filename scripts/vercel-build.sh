#!/bin/bash
set -euo pipefail

echo "=== Vercel Build Script for Kotlin Calendar ==="

# 프로젝트 toolchain(jvmToolchain(21))·CI와 같은 JDK 21을 쓴다. Vercel 빌드 이미지는 Amazon Linux 2023(dnf).
# 빌드 이미지에 Java 11이 기본으로 깔려 있으므로 java 존재 여부가 아니라 메이저 버전으로 판단한다.
REQUIRED_JAVA_MAJOR=21

java_major() {
    "$1" -XshowSettings:properties -version 2>&1 \
        | awk -F'= ' '/java.specification.version/ {print $2}' \
        | cut -d. -f1
}

current_major=""
if command -v java &> /dev/null; then
    current_major="$(java_major java)"
fi

if [ "${current_major:-0}" -lt "$REQUIRED_JAVA_MAJOR" ]; then
    echo "Java ${current_major:-<none>} found, need ${REQUIRED_JAVA_MAJOR}. Installing Amazon Corretto ${REQUIRED_JAVA_MAJOR}..."
    if command -v dnf &> /dev/null; then
        dnf install -y "java-${REQUIRED_JAVA_MAJOR}-amazon-corretto-devel"
        export JAVA_HOME="/usr/lib/jvm/java-${REQUIRED_JAVA_MAJOR}-amazon-corretto"
    elif command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y "openjdk-${REQUIRED_JAVA_MAJOR}-jdk-headless"
        export JAVA_HOME="/usr/lib/jvm/java-${REQUIRED_JAVA_MAJOR}-openjdk-amd64"
    fi
    export PATH="${JAVA_HOME:-}/bin:$PATH"
fi

if [ "$(java_major java 2>/dev/null || echo 0)" != "$REQUIRED_JAVA_MAJOR" ]; then
    echo "ERROR: JDK ${REQUIRED_JAVA_MAJOR} could not be installed." >&2
    exit 1
fi

echo "Java version:"
java -version

chmod +x ./gradlew

echo "Building client webApp (Wasm)..."
./gradlew :client:webApp:wasmJsBrowserDistribution --no-daemon --stacktrace

echo "Build complete. Distribution files located at client/webApp/build/dist/wasmJs/productionExecutable"
