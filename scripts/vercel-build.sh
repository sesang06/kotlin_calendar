#!/bin/bash
set -euo pipefail

echo "=== Vercel Build Script for Kotlin Calendar ==="

# Check Java installation or install if necessary
if ! command -v java &> /dev/null; then
    echo "Java not found. Installing OpenJDK 17..."
    if command -v amazon-linux-extras &> /dev/null; then
        amazon-linux-extras install java-openjdk17 -y
    elif command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y openjdk-17-jdk-headless
    elif command -v yum &> /dev/null; then
        yum install -y java-17-openjdk-devel
    fi
fi

echo "Java version:"
java -version || true

# Make gradlew executable
chmod +x ./gradlew

# Build WebAssembly frontend distribution
echo "Building client webApp (Wasm)..."
./gradlew :client:webApp:wasmJsBrowserDistribution --no-daemon --stacktrace

echo "Build complete. Distribution files located at client/webApp/build/dist/wasmJs/productionExecutable"
