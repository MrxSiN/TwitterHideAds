# Changelog

All notable changes to Twitter Hide Ads are documented here.

## 2.1.0 - 2026-09-09

- Suppress promoted posts on every X surface instead of only the Home timeline. Post detail, search and any other surface that renders posts through the same Compose boundaries are now covered.
- Match the URT `promoted-` entry token at any `-` segment boundary rather than only at the start of the entry identifier. Only the Home timeline names a promoted entry `promoted-tweet-<id>-<hash>`; a surface that nests the post inside a module prefixes that module's own entry, so the same advertisement arrives as `conversationthread-<id>-promoted-tweet-<id>-<hash>` in a post detail and as `search-conversation-<id>-promoted-tweet-<id>-<hash>` in search. These compound identifiers failed the previous prefix test and rendered.
- Recognise a promoted-metadata field by any runtime class name ending in `PromotedMetadata` instead of only the validated `com.x.models.TimelinePromotedMetadata`, which X has obfuscated away since `12.22.0`.
- Accept nested entry identifiers when selecting which direct string field carries the entry ID, so post detail and search entries are reported in the logs instead of `unknown`.
- Raised the classifier schema version from `6` to `7`.
- Validated on device against X `12.23.1-prod.01` under Vector 2.2: 11 deoptimized boundaries installed, `enforcement=ACTIVE_ADAPTIVE`, promoted entries blocked before render on both the Home timeline and post detail, with normal posts and replies unaffected.
- Increased Android `versionCode` from `31` to `32`.

## 2.0.0 - 2026-09-06

- Migrated the module from the legacy `de.robv.android.xposed` API 82 to the modern libxposed API `102.0.0`, as implemented by [Vector](https://github.com/JingMatrix/Vector). Frameworks that only implement the legacy API can no longer load the module.
- Replaced the `IXposedHookLoadPackage` entry point with `ModuleMain extends XposedModule`, declared through `META-INF/xposed/java_init.list`, with `module.prop` and `scope.list` replacing the `xposedmodule`, `xposedminversion` and `xposedscope` manifest metadata and the `assets/xposed_init` entry.
- Replaced `XposedBridge.hookMethod` callbacks with interceptor-chain hookers: a promoted post is now suppressed by returning without calling `chain.proceed()`, and the Video Tab filter proceeds with a rebuilt argument array instead of mutating the original one.
- Replaced the reflective `XposedBridge.deoptimizeMethod` lookup with `XposedInterface.deoptimize`, and `XposedHelpers` with plain reflection in `Reflect` and the platform `PackageManager` APIs.
- Routed framework access through `ModuleRuntime`, which holds the `XposedInterface` attached to the module entry and carries logging, hooking and deoptimization.
- Raised `minSdk` from 24 to 26, which the modern API requires, and the Java source and target level from 11 to 17.
- Split `AdaptiveHookResolver` into candidate discovery (`BoundaryCandidateSource`), persistence (`AdaptiveBoundaryCache`) and ranking, and moved the action-graph fallback out of `BundledAdPatterns` into `PromotedActionScanner`. No file exceeds 400 lines and no function exceeds 80.
- Documented rootless installation through LSPatch, which embeds Vector into a patched APK and loads modern libxposed modules through the same runtime.
- Increased Android `versionCode` from `30` to `31`.

## 1.3.0 - 2026-09-05

- Restored Home timeline suppression on X `12.22.0-prod.01`, which had been failing open since X restructured its render boundaries.
- Resolved the active boundary as `com.x.jetfuel.v2.element.attribute.h.h(...)`, declared outside the post package.
- Identify render boundaries by the post model they render rather than by the package they are declared in. The adaptive search previously covered only `com.x.urt.items.post` and could not reach the timeline boundary.
- Extracted structural boundary recognition into `RenderBoundaryShape`, matching parameters by role instead of by fixed position. X 12.22.0 dropped the post-dependency parameter, which the previous fixed 7-parameter layout required.
- Accept both the `Composer, $changed` and `Composer, $changed, $default` compiler shapes.
- Hook every boundary at or above the activation threshold instead of demanding a single unique winner. The defaulted and non-defaulted entry points of one composable score identically, so the ambiguity guard resolved to no hook at all.
- Deoptimize resolved boundaries through LSPosed. ART inlines these composables into their callers, so hooks installed on them were never reached.
- Enabled the bounded action-graph promoted-post fallback for adaptively resolved boundaries, since `com.x.models.TimelinePromotedMetadata` no longer exists as a class in X 12.22.0.
- Package native libraries uncompressed. A deflated `libdexkit.so` cannot be loaded from inside the APK, so DexKit resolution had been failing with `UnsatisfiedLinkError` and falling back to a scan that found nothing.
- Added bounded per-boundary observation logging for diagnosing future X updates.
- Bumped the adaptive resolution cache format so existing installs rescan.
- Increased Android `versionCode` from `29` to `30`.

## 1.2.0 - 2026-08-01

- Added a custom Twitter Hide Ads launcher icon.
- Added legacy launcher assets for mdpi through xxxhdpi.
- Added adaptive, round, and monochrome themed icon resources.
- Dynamically resolves the direct URT state-copy method whose first parameter and return type match and whose second parameter is a Kotlin immutable list.
- Removes only direct promoted timeline-post entries from mixed normal/promoted video batches.
- Reconstructs a compatible immutable list and verifies the result before replacing the method argument.
- Caches the resolved method by X version and APK fingerprint.
- Fails open on ambiguous resolution, incompatible immutable-list copying or failed verification.
- Removed the broad experimental video hook stack.
- Added a read-only data-layer diagnostic that identified `com.x.urt.o0$d.a(...)` as the mixed Video Tab batch boundary on X 12.10.1.
- Added adaptive structural resolution for the Home timeline pre-render boundary.

## 1.1.0 - 2026-07-17

- Stable promoted-post suppression for X `12.8.0-release.0`.
- A dedicated X 12.8.0 compatibility profile using:
  - `com.x.urt.items.post.w5$a`
  - `com.x.urt.items.post.d7.e(...)`
  - `com.x.urt.items.post.e.a(...)`
  - `com.x.urt.items.post.d7.a(...)`
- Version-aware selection between the validated X 12.7.1 and X 12.8.0 hook profiles.
- Updated promoted-post model detection for the X 12.8.0 `w5$a` model.
- Replaced the temporary X 12.8.0 diagnostic scanner with exact, lightweight render hooks.
- Kept fallback hooks inactive unless the primary render boundary is unavailable.
- Updated release documentation and project metadata for the stable GitHub release.
- Increased Android `versionCode` from `18` to `22` across the development and release cycle.

## 1.0.0

- First stable release for X 12.7.1.
- Added promoted-post suppression before Compose rendering.


