#!/data/data/com.termux/files/usr/bin/bash

ROOT="$PWD"
REPORT="$ROOT/QUICKFILESTUDIO_REAL_AUDIT.txt"

{
echo "============================================================"
echo " QUICK FILE STUDIO - REAL AUDIT"
echo " $(date)"
echo "============================================================"

echo
echo "========== JAVASCRIPT REAL SYNTAX =========="

JS_ERRORS=0
while IFS= read -r -d '' f; do
    if command -v node >/dev/null 2>&1; then
        ERR=$(node --check "$f" 2>&1)
        CODE=$?
        if [ "$CODE" -ne 0 ]; then
            echo
            echo "ERROR: $f"
            echo "$ERR"
            JS_ERRORS=$((JS_ERRORS+1))
        else
            echo "OK: $f"
        fi
    else
        echo "WARNING: node غير مثبت"
        break
    fi
done < <(find web -type f -name '*.js' -print0 2>/dev/null)

echo
echo "REAL JS ERRORS: $JS_ERRORS"

echo
echo "========== MISSING LOCAL SCRIPT FILES =========="
python3 - <<'PY'
import re, os

f="web/index.html"
if os.path.isfile(f):
    s=open(f,encoding="utf-8",errors="ignore").read()
    refs=re.findall(r'<script[^>]+src=["\']([^"\']+)["\']',s,re.I)
    for r in refs:
        if r.startswith(("http://","https://","//")):
            print("EXTERNAL:",r)
        else:
            p=os.path.normpath(os.path.join("web",r))
            print(("OK: " if os.path.isfile(p) else "MISSING: ")+p)
PY

echo
echo "========== DUPLICATE HTML IDs =========="
python3 - <<'PY'
import re,collections,os
f="web/index.html"
if os.path.isfile(f):
    s=open(f,encoding="utf-8",errors="ignore").read()
    ids=re.findall(r'\bid=["\']([^"\']+)["\']',s,re.I)
    c=collections.Counter(ids)
    found=False
    for k,v in sorted(c.items()):
        if v>1:
            print(f"DUPLICATE ID: {k} ({v}x)")
            found=True
    if not found:
        print("NONE")
PY

echo
echo "========== REAL DUPLICATE TOP-LEVEL JS FUNCTIONS =========="
python3 - <<'PY'
import re,glob,collections

for f in glob.glob("web/js/**/*.js",recursive=True):
    try:
        s=open(f,encoding="utf-8",errors="ignore").read()
    except:
        continue

    # فقط function declarations، وليس الكلمات الموجودة داخل النصوص
    names=re.findall(r'(?m)^\s*(?:async\s+)?function\s+([A-Za-z_$][\w$]*)\s*\(',s)
    c=collections.Counter(names)

    for n,v in sorted(c.items()):
        if v>1:
            print(f"{f}: {n} ({v} declarations)")
PY

echo
echo "========== UNDEFINED LOCAL SCRIPT REFERENCES =========="
python3 - <<'PY'
import re,glob,os

for f in glob.glob("web/**/*.html",recursive=True):
    if ".backup" in f or ".bak" in f or ".before-" in f:
        continue
    try:
        s=open(f,encoding="utf-8",errors="ignore").read()
    except:
        continue

    for r in re.findall(r'<script[^>]+src=["\']([^"\']+)["\']',s,re.I):
        if r.startswith(("http://","https://","//")):
            continue
        p=os.path.normpath(os.path.join(os.path.dirname(f),r))
        if not os.path.isfile(p):
            print(f"MISSING: {f} -> {r}")
PY

echo
echo "========== REAL PLACEHOLDER / FAKE URL CHECK =========="
grep -RniE \
--exclude='*.backup*' \
--exclude='*.bak' \
--exclude='*.before-*' \
--exclude='QUICKFILESTUDIO_*AUDIT*' \
-E 'YOUR-DOMAIN|example\.com|example\.org|TODO|FIXME|IMPLEMENT.?ME|not.?implemented|fake|dummy|placeholder' \
web android app 2>/dev/null || true

echo
echo "========== DEBUG / CONSOLE =========="
grep -RniE \
--exclude='*.backup*' \
--exclude='*.bak' \
--exclude='*.before-*' \
-E 'console\.(log|debug|error|warn)\(' \
web android app 2>/dev/null || true

echo
echo "========== EMPTY FUNCTIONS =========="
python3 - <<'PY'
import re,glob
for f in glob.glob("web/js/**/*.js",recursive=True):
    try:s=open(f,encoding="utf-8",errors="ignore").read()
    except:continue
    for m in re.finditer(r'function\s+([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*\{\s*\}',s):
        print(f"{f}: EMPTY FUNCTION {m.group(1)}")
PY

echo
echo "========== CURRENT SOURCE FILES =========="
find web/js android app -type f 2>/dev/null | sort

echo
echo "============================================================"
echo " END REAL AUDIT"
echo "============================================================"

} > "$REPORT"

cat "$REPORT"
echo
echo "REPORT: $REPORT"
