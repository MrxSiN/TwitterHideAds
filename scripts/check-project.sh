#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_GRADLE="$ROOT/app/build.gradle.kts"
WORKFLOW="$ROOT/.github/workflows/android.yml"
INIT="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/XposedInit.java"
HOOK="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/TwitterAdBlocker.java"
PROFILE="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/CompatibilityProfile.java"
PATTERNS="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/BundledAdPatterns.java"
PATTERN_JSON="$ROOT/app/src/main/assets/ad_patterns.json"

for file in "$APP_GRADLE" "$WORKFLOW" "$INIT" "$HOOK" "$PROFILE" "$PATTERNS" "$PATTERN_JSON"; do
  test -f "$file"
done

grep -q 'val appVersion = "1.1.0"' "$APP_GRADLE"
grep -q 'versionCode = 22' "$APP_GRADLE"
grep -q 'compileSdk = 36' "$APP_GRADLE"
grep -q 'targetSdk = 36' "$APP_GRADLE"
grep -q 'id("com.android.application") version "9.2.1"' "$ROOT/build.gradle.kts"
grep -q 'gradle-9.4.1-bin.zip' "$ROOT/gradle/wrapper/gradle-wrapper.properties"
grep -q 'r0adkll/sign-android-release@v1' "$WORKFLOW"
! grep -q 'signingConfigs' "$APP_GRADLE"

grep -q 'selectedProfile=' "$INIT"
grep -q 'ACTIVE_PRIMARY' "$HOOK"
grep -q 'com.x.urt.items.post.d7' "$PROFILE"
grep -q 'com.x.urt.items.post.w5$a' "$PROFILE"
grep -q 'com.x.urt.items.post.c7' "$PROFILE"
grep -q 'com.x.urt.items.post.a6$a' "$PROFILE"
grep -q 'promoted-' "$PATTERNS"
grep -q 'TimelinePromotedMetadata' "$PATTERNS"
grep -q '"postSuppression": true' "$PATTERN_JSON"
! grep -q 'new DexFile' "$HOOK"

echo "Static project checks passed."
