#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

fail() {
  echo "Project check failed: $*" >&2
  exit 1
}

APP_VERSION="$(sed -n 's/^val appVersion = "\([^"]*\)"/\1/p' app/build.gradle.kts)"
APP_VERSION_CODE="$(sed -n 's/^val appVersionCode = \([0-9][0-9]*\)/\1/p' app/build.gradle.kts)"

[[ -n "$APP_VERSION" ]] || fail "appVersion is missing"
[[ "$APP_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "appVersion must use semantic versioning"
[[ -n "$APP_VERSION_CODE" ]] || fail "appVersionCode is missing"

required_files=(
  README.md
  CHANGELOG.md
  HOOK_NOTES.md
  RELEASE_NOTES.md
  LICENSE
  NOTICE.md
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/assets/xposed_init
  app/src/main/assets/ad_patterns.json
  app/src/main/res/values/arrays.xml
  .github/workflows/android.yml
)

for path in "${required_files[@]}"; do
  [[ -f "$path" ]] || fail "missing $path"
done

grep -Fq 'applicationId = "my.MrxSiN.twitterhideads"' app/build.gradle.kts \
  || fail "unexpected applicationId"
grep -Fq 'com.twitter.android' app/src/main/res/values/arrays.xml \
  || fail "X package is missing from LSPosed scope"
[[ "$(grep -c '<item>' app/src/main/res/values/arrays.xml)" -eq 1 ]] \
  || fail "LSPosed scope must contain exactly one package"
grep -Fq "private static final String MODULE_VERSION = \"$APP_VERSION\";" \
  app/src/main/java/my/MrxSiN/twitterhideads/XposedInit.java \
  || fail "XposedInit version does not match appVersion"
grep -Fq "## $APP_VERSION" CHANGELOG.md \
  || fail "CHANGELOG does not contain the current version"
grep -Fq "v$APP_VERSION" RELEASE_NOTES.md \
  || fail "RELEASE_NOTES does not contain the current version"

python3 - "$APP_VERSION" <<'PY'
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

version = sys.argv[1]
root = Path('.')

asset = json.loads((root / 'app/src/main/assets/ad_patterns.json').read_text())
assert asset['moduleVersion'] == version, 'asset moduleVersion mismatch'
assert asset['releaseChannel'] == 'stable', 'release channel must be stable'
assert asset['targetPackage'] == 'com.twitter.android', 'target package mismatch'
assert asset['compatibilityProfiles'], 'no compatibility profile defined'

for xml in (root / 'app/src/main').rglob('*.xml'):
    ET.parse(xml)
PY

bash -n scripts/check-project.sh

echo "Project checks passed: version=$APP_VERSION versionCode=$APP_VERSION_CODE"
