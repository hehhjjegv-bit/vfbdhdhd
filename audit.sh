#!/data/data/com.termux/files/usr/bin/bash

ROOT="$(pwd)"
REPORT="$ROOT/QUICKFILESTUDIO_AUDIT.txt"

{
echo "=================================================="
echo " QUICK FILE STUDIO - FULL AUDIT"
echo " DATE: $(date)"
echo " ROOT: $ROOT"
echo "=================================================="
echo

echo "========== FILES =========="
find . -type f \
  ! -path './.git/*' \
  ! -path './node_modules/*' \
  ! -name 'QUICKFILESTUDIO_AUDIT.txt' \
  | sort
echo

echo "========== JAVASCRIPT SYNTAX =========="
JSCOUNT=0
JSERROR=0
while IFS= read -r f; do
  JSCOUNT=$((JSCOUNT+1))
  if command -v node >/dev/null 2>&1; then
    if node --check "$f" >/tmp/qfs_js_error 2>&1; then
      echo "OK      $f"
    else
      JSERROR=$((JSERROR+1))
      echo "ERROR   $f"
      cat /tmp/qfs_js_error
    fi
  fi
done < <(find web -type f -name '*.js' 2>/dev/null | sort)

echo
echo "JS FILES : $JSCOUNT"
echo "JS ERRORS: $JSERROR"
echo

echo "========== HTML =========="
if [ -f web/index.html ]; then
  if command -v tidy >/dev/null 2>&1; then
    tidy -errors -quiet web/index.html 2>&1 || true
  else
    echo "tidy غير مثبت - تم إجراء الفحوص الأساسية فقط"
  fi
fi
echo

echo "========== DUPLICATE IDs =========="
if [ -f web/index.html ]; then
  grep -oE 'id=["'\''][^"'\'']+["'\'']' web/index.html \
  | sed -E 's/^id=["'\'']//;s/["'\'']$//' \
  | sort | uniq -c | sort -nr | awk '$1>1 {print "DUPLICATE:",$0}'
fi
echo

echo "========== DUPLICATE FUNCTION DEFINITIONS =========="
grep -RhoE '^[[:space:]]*(async[[:space:]]+)?function[[:space:]]+[A-Za-z_$][A-Za-z0-9_$]*[[:space:]]*\(' \
web --include='*.js' 2>/dev/null \
| sed -E 's/^[[:space:]]*(async[[:space:]]+)?function[[:space:]]+//;s/[[:space:]]*\($//' \
| sort | uniq -c | sort -nr \
| awk '$1>1 {print "POSSIBLE DUPLICATE:",$0}'
echo

echo "========== SCRIPT REFERENCES =========="
if [ -f web/index.html ]; then
  grep -nE '<script[^>]+src=' web/index.html || true
fi
echo

echo "========== MISSING LOCAL SCRIPT FILES =========="
if [ -f web/index.html ]; then
  grep -oE '<script[^>]+src=["'\''][^"'\'']+["'\'']' web/index.html \
  | sed -E 's/.*src=["'\'']([^"'\'']+)["'\''].*/\1/' \
  | while IFS= read -r src; do
      case "$src" in
        http://*|https://*|//*) continue ;;
      esac
      if [ ! -f "web/$src" ]; then
        echo "MISSING SCRIPT: web/$src"
      fi
    done
fi
echo

echo "========== LOCAL RESOURCE REFERENCES =========="
grep -RniE 'src=["'\'']|href=["'\'']' web/index.html 2>/dev/null \
| grep -vE 'https?://|data:|#' \
| head -500 || true
echo

echo "========== EMPTY / SUSPICIOUS HANDLERS =========="
grep -RniE 'TODO|FIXME|not implemented|coming soon|demo only|dummy|fake|placeholder|محاكاة|وهمي|غير متاح' \
web 2>/dev/null || true
echo

echo "========== ALERT-ONLY / PLACEHOLDER FUNCTIONS =========="
grep -RniE 'alert[[:space:]]*\(|toast[[:space:]]*\([^)]*(غير متوفر|غير مدعوم|قريب|وهمي|تجريبي)' \
web --include='*.js' --include='*.html' 2>/dev/null || true
echo

echo "========== BUTTONS =========="
if [ -f web/index.html ]; then
  grep -nE '<button|onclick=' web/index.html | head -500 || true
fi
echo

echo "========== EVENT HANDLERS =========="
grep -RniE 'addEventListener|onclick[[:space:]]*=|onchange[[:space:]]*=|oninput[[:space:]]*=|onsubmit[[:space:]]*=' \
web --include='*.js' --include='*.html' 2>/dev/null | head -1000 || true
echo

echo "========== EMPTY FUNCTIONS =========="
grep -RniE '\{[[:space:]]*\}' web --include='*.js' 2>/dev/null | head -300 || true
echo

echo "========== CONSOLE / DEBUG =========="
grep -RniE 'console\.(log|debug|warn|error)[[:space:]]*\(' \
web --include='*.js' 2>/dev/null | head -500 || true
echo

echo "========== COMMON BROKEN PATTERNS =========="
grep -RniE 'undefined|NaN|innerHTML[[:space:]]*=[[:space:]]*""|return[[:space:]]*false|TODO|FIXME' \
web --include='*.js' 2>/dev/null | head -1000 || true
echo

echo "========== UNMATCHED BRACES (ROUGH CHECK) =========="
while IFS= read -r f; do
  opens=$(grep -o '{' "$f" 2>/dev/null | wc -l)
  closes=$(grep -o '}' "$f" 2>/dev/null | wc -l)
  if [ "$opens" != "$closes" ]; then
    echo "BRACE MISMATCH: $f  {=$opens }=$closes"
  fi
done < <(find web -type f -name '*.js' 2>/dev/null)
echo

echo "========== UNMATCHED PARENTHESES (ROUGH CHECK) =========="
while IFS= read -r f; do
  opens=$(grep -o '(' "$f" 2>/dev/null | wc -l)
  closes=$(grep -o ')' "$f" 2>/dev/null | wc -l)
  if [ "$opens" != "$closes" ]; then
    echo "PAREN MISMATCH: $f  (=$opens )=$closes"
  fi
done < <(find web -type f -name '*.js' 2>/dev/null)
echo

echo "========== FILES WITH VERY LARGE SIZE =========="
find web -type f -printf '%s %p\n' 2>/dev/null \
| sort -nr | head -30
echo

echo "=================================================="
echo " AUDIT FINISHED"
echo " REPORT: $REPORT"
echo "=================================================="

} > "$REPORT"

cat "$REPORT"
