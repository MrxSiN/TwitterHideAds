#!/usr/bin/env sh
# Static checks the build and the tests cannot make: version consistency,
# signing hygiene, supply chain and the Xposed module declaration. Code
# behaviour is covered by the tests; generated-file staleness by
# `python3 tools/bftool/gen.py --check`.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_GRADLE="$ROOT/app/build.gradle.kts"
WORKFLOW="$ROOT/.github/workflows/android.yml"
WRAPPER_PROPS="$ROOT/gradle/wrapper/gradle-wrapper.properties"
MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"
README="$ROOT/README.md"
CHANGELOG="$ROOT/CHANGELOG.md"
XPOSED_META="$ROOT/app/src/main/resources/META-INF/xposed"

# "! cmd" is exempt from set -e, so negative checks must fail explicitly.
refute() {
  if "$@"; then
    echo "check-project: unexpected match: $*" >&2
    exit 1
  fi
}

committed_keystore() {
  git -C "$ROOT" ls-files | grep -Eq '\.(jks|keystore|p12)$'
}

unpinned_action() {
  grep -E '^ *(- )?uses: ' "$WORKFLOW" | grep -Evq '@[0-9a-f]{40}( |$)'
}

# The version is declared once, in app/build.gradle.kts; the docs must agree.
APP_VERSION=$(sed -n 's/^val appVersion = "\([^"]*\)"$/\1/p' "$APP_GRADLE")
VERSION_CODE=$(sed -n 's/^ *versionCode = \([0-9][0-9]*\)$/\1/p' "$APP_GRADLE")
test -n "$APP_VERSION"
test -n "$VERSION_CODE"
grep -q 'versionName = appVersion' "$APP_GRADLE"
grep -q "^## $APP_VERSION " "$CHANGELOG"
grep -Fq "\`$APP_VERSION\` (\`versionCode $VERSION_CODE\`)" "$README"
refute grep -qi 'entirely written in Brainfuck' "$README"

grep -q 'compileOnly("io.github.libxposed:api:102.0.0")' "$APP_GRADLE"
grep -q 'implementation("org.luckypray:dexkit:2.2.0")' "$APP_GRADLE"
grep -q 'merges += "META-INF/xposed/\*"' "$APP_GRADLE"
grep -q 'minSdk = 26' "$APP_GRADLE"

# Release signing is environment-backed; no credential may be committed.
grep -q 'System.getenv("ANDROID_KEYSTORE_PASSWORD")' "$APP_GRADLE"
refute grep -Eq '(storePassword|keyPassword|keyAlias) *= *"' "$APP_GRADLE"
refute grep -Eq 'storeFile *= *file\( *"' "$APP_GRADLE"
refute committed_keystore

# Supply chain: verified Gradle distribution, actions pinned to commit SHAs,
# and CI runs the checks this script does not.
grep -Eq '^distributionSha256Sum=[0-9a-f]{64}$' "$WRAPPER_PROPS"
refute unpinned_action
grep -q 'testDebugUnitTest' "$WORKFLOW"
grep -q 'gen.py --check' "$WORKFLOW"
grep -q 'unittest discover' "$WORKFLOW"
grep -q 'libtwitterbf.so' "$WORKFLOW"

# Modern Xposed API module declaration; no legacy entry point or metadata.
grep -q '^my.MrxSiN.twitterhideads.ModuleMain$' "$XPOSED_META/java_init.list"
grep -q '^com.twitter.android$' "$XPOSED_META/scope.list"
grep -q '^minApiVersion=' "$XPOSED_META/module.prop"
grep -q '^targetApiVersion=102$' "$XPOSED_META/module.prop"
refute test -f "$ROOT/app/src/main/assets/xposed_init"
refute grep -q 'xposedmodule\|xposedminversion\|xposedscope' "$MANIFEST"
refute grep -rq 'de.robv.android.xposed' "$ROOT/app/src/main/java"

echo "Static project checks passed."
