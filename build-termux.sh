#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"

echo "======================================"
echo " Quick File Studio"
echo "======================================"
echo "Project: $ROOT"
echo "SDK:     $ANDROID_SDK_ROOT"
echo

command -v java >/dev/null 2>&1 || {
    echo "Java غير موجود"
    exit 1
}

command -v curl >/dev/null 2>&1 || {
    echo "curl غير موجود"
    exit 1
}

command -v unzip >/dev/null 2>&1 || {
    echo "unzip غير موجود"
    exit 1
}

test -f "$ANDROID_SDK_ROOT/platforms/android-35/android.jar" || {
    echo "Android Platform 35 غير موجود"
    exit 1
}

test -d "$ANDROID_SDK_ROOT/build-tools/35.0.0" || {
    echo "Build Tools 35.0.0 غير موجود"
    exit 1
}

echo "[1/4] Sync web -> Android assets"

mkdir -p android/app/src/main/assets/www/css
mkdir -p android/app/src/main/assets/www/js

cp -f web/index.html \
android/app/src/main/assets/www/index.html

cp -f web/css/app.css \
android/app/src/main/assets/www/css/app.css

cp -f web/js/app.js \
android/app/src/main/assets/www/js/app.js

echo "[2/4] Checking Gradle"

GRADLE_VERSION="8.9"
GRADLE_DIR="$HOME/.qfs-gradle/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$HOME/.qfs-gradle"

    TMP="$HOME/.qfs-gradle/gradle-$GRADLE_VERSION-bin.zip"

    echo "Downloading Gradle $GRADLE_VERSION..."

    curl -L --fail --retry 3 \
        -o "$TMP" \
        "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

    rm -rf "$GRADLE_DIR"

    unzip -q "$TMP" -d "$HOME/.qfs-gradle"

    rm -f "$TMP"
fi

echo "[3/4] Building"

"$GRADLE_BIN" \
    --no-daemon \
    --stacktrace \
    assembleDebug

APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"

echo
echo "[4/4] BUILD SUCCESS"
echo
echo "APK:"
echo "$APK"
echo

if [ -f "$APK" ]; then
    cp -f "$APK" \
        "$HOME/storage/downloads/QuickFileStudio.apk"

    echo "Copied to:"
    echo "$HOME/storage/downloads/QuickFileStudio.apk"
fi
