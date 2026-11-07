#!/usr/bin/env bash
# Publishes CrashGuard to Maven Local and (optionally) tags a JitPack release.
#
# Usage:
#   ./scripts/publish.sh              # publish to ~/.m2 only
#   ./scripts/publish.sh --tag        # publish + create and push git tag v<VERSION>
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="$(grep -E '^VERSION_NAME=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
GROUP="$(grep -E '^GROUP=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
ARTIFACT="$(grep -E '^POM_ARTIFACT_ID=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"

echo "Publishing ${GROUP}:${ARTIFACT}:${VERSION} to Maven Local..."
chmod +x gradlew
./gradlew :crash-guard:publishReleasePublicationToMavenLocal

echo "Published to: $HOME/.m2/repository/${GROUP//.//}/${ARTIFACT}/${VERSION}/"

if [ "${1:-}" = "--tag" ]; then
  echo "Creating git tag v${VERSION}..."
  git tag -a "v${VERSION}" -m "Release v${VERSION}"
  git push origin "v${VERSION}"
  echo "Tagged v${VERSION}. JitPack will build on the first dependency request."
fi
