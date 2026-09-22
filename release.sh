#!/data/data/com.termux/files/usr/bin/bash

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

cd "$ROOT"

VERSION="${1:-}"

if [ -z "$VERSION" ]; then
    echo
    echo "الاستخدام:"
    echo
    echo "./release.sh 1.1.0"
    echo
    exit 1
fi

VERSION_CODE="${VERSION//./}"

echo "======================================"
echo " Quick File Studio Release"
echo "======================================"
echo
echo "Version: $VERSION"
echo "Code:    $VERSION_CODE"
echo

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"

echo "[1/7] تحديث رقم الإصدار"

python - "$VERSION" "$VERSION_CODE" <<'PY'
import sys
from pathlib import Path

version = sys.argv[1]
code = int(sys.argv[2])

p = Path("android/app/build.gradle")
s = p.read_text()

import re

s = re.sub(
    r'versionCode\s+\d+',
    f'versionCode {code}',
    s
)

s = re.sub(
    r'versionName\s+"[^"]+"',
    f'versionName "{version}"',
    s
)

p.write_text(s)

print("تم تحديث build.gradle")
PY

echo "[2/7] مزامنة Web"

mkdir -p android/app/src/main/assets/www
mkdir -p android/app/src/main/assets/www/css
mkdir -p android/app/src/main/assets/www/js
mkdir -p android/app/src/main/assets/www/locales

cp -f web/index.html \
    android/app/src/main/assets/www/index.html

cp -f web/css/app.css \
    android/app/src/main/assets/www/css/app.css

cp -f web/js/*.js \
    android/app/src/main/assets/www/js/

cp -f web/locales/*.json \
    android/app/src/main/assets/www/locales/

echo "[3/7] تنظيف"

rm -rf android/app/build

echo "[4/7] البناء"

chmod +x build-termux.sh

./build-termux.sh

APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo
    echo "ERROR: APK غير موجود"
    exit 1
fi

echo "[5/7] حساب SHA-256"

SHA256="$(sha256sum "$APK" | awk '{print $1}')"

echo "$SHA256"

echo "[6/7] إنشاء Release"

mkdir -p releases

OUT="$ROOT/releases/QuickFileStudio-$VERSION.apk"

cp -f "$APK" "$OUT"

cat > "releases/update.json" <<JSON
{
  "versionCode": $VERSION_CODE,
  "versionName": "$VERSION",
  "mandatory": false,
  "title": "يتوفر تحديث جديد",
  "message": "يتوفر إصدار جديد من Quick File Studio.",
  "releaseNotes": [
    "تحسينات عامة",
    "تحسين مدير الملفات",
    "إصلاح الأخطاء"
  ],
  "apkUrl": "",
  "sha256": "$SHA256"
}
JSON

cp -f \
    releases/update.json \
    update/version.json

echo "[7/7] مكتمل"

echo
echo "======================================"
echo " RELEASE SUCCESS"
echo "======================================"
echo
echo "APK:"
echo "$OUT"
echo
echo "SHA256:"
echo "$SHA256"
echo
echo "Update:"
echo "$ROOT/releases/update.json"
echo
