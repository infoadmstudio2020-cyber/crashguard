#!/usr/bin/env bash
# Builds the CrashGuard library AAR and the demo application.
set -euo pipefail

cd "$(dirname "$0")/.."

chmod +x gradlew

echo "==> Cleaning"
./gradlew clean

echo "==> Building library AAR"
./gradlew :crash-guard:assembleRelease

echo "==> Running unit tests"
./gradlew :crash-guard:testDebugUnitTest

echo "==> Building demo app (debug + release)"
./gradlew :app:assembleDebug :app:assembleRelease

echo
echo "Artifacts:"
echo "  AAR : crash-guard/build/outputs/aar/crash-guard-release.aar"
echo "  APK : app/build/outputs/apk/debug/app-debug.apk"
echo "  APK : app/build/outputs/apk/release/app-release.apk"
