#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP_GRADLE="$ROOT/app/build.gradle.kts"
WORKFLOW="$ROOT/.github/workflows/android.yml"
MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"
INIT="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/ModuleMain.java"
RUNTIME="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/ModuleRuntime.java"
BLOCKER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/TwitterAdBlocker.java"
RESOLVER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/AdaptiveHookResolver.java"
CANDIDATES="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/BoundaryCandidateSource.java"
CACHE="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/AdaptiveBoundaryCache.java"
VIDEO_RESOLVER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetResolver.java"
VIDEO_FILTER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetFilter.java"
VIDEO_CLASSIFIER="$ROOT/app/src/main/java/my/MrxSiN/twitterhideads/VideoDatasetClassifier.java"
PATTERN_JSON="$ROOT/app/src/main/assets/ad_patterns.json"
XPOSED_META="$ROOT/app/src/main/resources/META-INF/xposed"

for file in "$APP_GRADLE" "$WORKFLOW" "$MANIFEST" "$INIT" "$RUNTIME" "$BLOCKER" \
  "$RESOLVER" "$CANDIDATES" "$CACHE" "$VIDEO_RESOLVER" "$VIDEO_FILTER" \
  "$VIDEO_CLASSIFIER" "$PATTERN_JSON" "$XPOSED_META/java_init.list" \
  "$XPOSED_META/module.prop" "$XPOSED_META/scope.list"; do
  test -f "$file"
done

grep -q 'val appVersion = "2.1.0"' "$APP_GRADLE"
grep -q 'versionCode = 33' "$APP_GRADLE"
grep -q 'compileOnly("io.github.libxposed:api:102.0.0")' "$APP_GRADLE"
grep -q 'implementation("org.luckypray:dexkit:2.2.0")' "$APP_GRADLE"
grep -q 'merges += "META-INF/xposed/\*"' "$APP_GRADLE"
grep -q 'minSdk = 26' "$APP_GRADLE"
grep -q 'r0adkll/sign-android-release@v1' "$WORKFLOW"
! grep -q 'signingConfigs' "$APP_GRADLE"

# Modern Xposed API module declaration. The legacy assets entry point and the
# xposed* manifest metadata are replaced by META-INF/xposed resources.
grep -q '^my.MrxSiN.twitterhideads.ModuleMain$' "$XPOSED_META/java_init.list"
grep -q '^com.twitter.android$' "$XPOSED_META/scope.list"
grep -q '^minApiVersion=' "$XPOSED_META/module.prop"
grep -q '^targetApiVersion=102$' "$XPOSED_META/module.prop"
! test -f "$ROOT/app/src/main/assets/xposed_init"
! grep -q 'xposedmodule\|xposedminversion\|xposedscope' "$MANIFEST"
! grep -rq 'de.robv.android.xposed' "$ROOT/app/src/main/java"

grep -q 'extends XposedModule' "$INIT"
grep -q 'MODULE_VERSION = "2.1.0"' "$INIT"
grep -q 'onPackageReady' "$INIT"
grep -q 'VideoDatasetResolver.resolve' "$INIT"
grep -q 'VideoDatasetFilter.install' "$INIT"
grep -q 'api.deoptimize' "$RUNTIME"
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

grep -q '"moduleVersion": "2.1.0"' "$PATTERN_JSON"
grep -q '"installedHooks": 1' "$PATTERN_JSON"
grep -q '"composeHooks": 0' "$PATTERN_JSON"
grep -q '"playbackHooks": 0' "$PATTERN_JSON"

echo "Static active video-dataset filter checks passed."
