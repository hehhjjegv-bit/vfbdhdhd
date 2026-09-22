#!/usr/bin/env python3

import base64
import getpass
import hashlib
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KEYSTORE = Path.home() / "qfs-private-signing/qfs-update-signing.jks"
ALIAS = "qfs-update-signing"
JAVA_CP = Path.home() / "qfs-java-test"


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while True:
            chunk = f.read(1024 * 1024)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()


def main():
    if len(sys.argv) != 2:
        print("الاستخدام:")
        print("  python3 tools/sign-update.py <APK>")
        sys.exit(2)

    apk = Path(sys.argv[1]).expanduser().resolve()

    if not apk.is_file():
        print("خطأ: ملف APK غير موجود:")
        print(apk)
        sys.exit(1)

    if not KEYSTORE.is_file():
        print("خطأ: ملف توقيع التحديث غير موجود:")
        print(KEYSTORE)
        sys.exit(1)

    sha256 = sha256_file(apk)

    print()
    print("APK:")
    print(apk)
    print()
    print("SHA-256:")
    print(sha256)
    print()

    package_name = "com.quickfilestudio.app"

    version_code = input("Version code: ").strip()
    version_name = input("Version name: ").strip()
    apk_url = input("APK URL: ").strip()

    if not version_code.isdigit():
        print("خطأ: Version code يجب أن يكون رقمًا.")
        sys.exit(1)

    if not version_name:
        print("خطأ: Version name فارغ.")
        sys.exit(1)

    if not apk_url.startswith(("https://", "http://")):
        print("خطأ: APK URL غير صحيح.")
        sys.exit(1)

    signed_data = (
        "QFS-UPDATE-V1\n"
        + package_name + "\n"
        + version_code + "\n"
        + version_name + "\n"
        + apk_url + "\n"
        + sha256.lower()
    )

    with tempfile.TemporaryDirectory(prefix="qfs-sign-") as tmp:
        tmp = Path(tmp)

        data_file = tmp / "signed-data.txt"
        signature_file = tmp / "signature.txt"

        data_file.write_text(signed_data, encoding="utf-8")

        password = getpass.getpass("Keystore password: ")

        result = subprocess.run(
            [
                "java",
                "-cp",
                str(JAVA_CP),
                "UpdateSigner",
                str(KEYSTORE),
                ALIAS,
                str(data_file),
                str(signature_file),
            ],
            input=password + "\n",
            text=True,
            capture_output=True,
        )

        if result.returncode != 0:
            print("فشل التوقيع.")
            if result.stderr:
                print(result.stderr)
            sys.exit(1)

        if not signature_file.is_file():
            print("خطأ: لم يتم إنشاء ملف التوقيع.")
            sys.exit(1)

        signature = signature_file.read_text(
            encoding="ascii"
        ).strip()

        if not signature:
            print("خطأ: التوقيع فارغ.")
            sys.exit(1)

        output = {
            "packageName": package_name,
            "versionCode": int(version_code),
            "versionName": version_name,
            "apkUrl": apk_url,
            "sha256": sha256,
            "signature": signature
        }

        output_path = ROOT / "version.json"

        output_path.write_text(
            json.dumps(
                output,
                ensure_ascii=False,
                indent=2
            ) + "\n",
            encoding="utf-8"
        )

    print()
    print("تم إنشاء ملف التحديث:")
    print(output_path)
    print()
    print("المفتاح الخاص بقي خارج المشروع.")


if __name__ == "__main__":
    main()
