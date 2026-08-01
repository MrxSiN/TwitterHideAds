#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_GRADLE="$ROOT/app/build.gradle.kts"
WORKFLOW="$ROOT/.github/workflows/android.yml"
INIT="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/XposedInit.java"
BLOCKER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/TwitterAdBlocker.java"
RESOLVER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/AdaptiveHookResolver.java"
VIDEO_RESOLVER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetResolver.java"
VIDEO_FILTER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetFilter.java"
VIDEO_CLASSIFIER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetClassifier.java"
PATTERN_JSON="$ROOT/app/src/main/assets/ad_patterns.json"

for file in "$APP_GRADLE" "$WORKFLOW" "$INIT" "$BLOCKER" "$RESOLVER" \
  "$VIDEO_RESOLVER" "$VIDEO_FILTER" "$VIDEO_CLASSIFIER" "$PATTERN_JSON"; do
  test -f "$file"
done

grep -q 'val appVersion = "1.2.6-test-app-icon"' "$APP_GRADLE"
grep -q 'versionCode = 29' "$APP_GRADLE"
grep -q 'implementation("org.luckypray:dexkit:2.2.0")' "$APP_GRADLE"
grep -q 'r0adkll/sign-android-release@v1' "$WORKFLOW"
! grep -q 'signingConfigs' "$APP_GRADLE"

grep -q 'MODULE_VERSION = "1.2.6-test-app-icon"' "$INIT"
grep -q 'VideoDatasetResolver.resolve' "$INIT"
grep -q 'VideoDatasetFilter.install' "$INIT"
grep -q 'installedHooks=1' "$VIDEO_FILTER"
grep -q 'calledFromVideoTab' "$VIDEO_FILTER"
grep -q 'safeMixedVideoBatch' "$VIDEO_FILTER"
grep -q 'Filtered promoted videos before pager creation' "$VIDEO_FILTER"
grep -q 'first parameter equals return type\|parameters\[0\] != returnType' "$VIDEO_RESOLVER"
grep -q 'kotlinx.collections.immutable.' "$VIDEO_RESOLVER"
grep -q 'promotedCount != 0' "$VIDEO_FILTER"

! test -f "$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetDiagnostic.java"
! test -f "$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetInspector.java"
! grep -q 'setPlayWhenReady' "$VIDEO_FILTER"
! grep -Eq '\.pause\(' "$VIDEO_FILTER"
! grep -q 'androidx.compose' "$VIDEO_FILTER"
! grep -q 'com.x.media.autoplay' "$VIDEO_FILTER"
! grep -q 'com.x.media.playback' "$VIDEO_FILTER"

grep -q '"moduleVersion": "1.2.6-test-app-icon"' "$PATTERN_JSON"
grep -q '"installedHooks": 1' "$PATTERN_JSON"
grep -q '"composeHooks": 0' "$PATTERN_JSON"
grep -q '"playbackHooks": 0' "$PATTERN_JSON"

echo "Static active video-dataset filter checks passed."
