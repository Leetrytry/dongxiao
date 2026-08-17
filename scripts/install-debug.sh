#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export HOME="$ROOT_DIR/.tools/adb-home"
export ANDROID_SDK_ROOT="$ROOT_DIR/.tools/android-sdk"

mkdir -p "$HOME"

APK_PATH="${1:-$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk}"

exec "$ANDROID_SDK_ROOT/platform-tools/adb" install -r "$APK_PATH"
