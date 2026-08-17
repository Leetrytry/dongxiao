#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export JAVA_HOME="$ROOT_DIR/.tools/jdk-17.0.20+8/Contents/Home"
export ANDROID_SDK_ROOT="$ROOT_DIR/.tools/android-sdk"
export GRADLE_USER_HOME="$ROOT_DIR/.tools/gradle-home"

if [[ $# -eq 0 ]]; then
  set -- assembleDebug
fi

exec "$ROOT_DIR/.tools/gradle-8.10.2/bin/gradle" --no-daemon "$@"
