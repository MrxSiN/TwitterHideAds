# Changelog

All notable changes to Twitter Hide Ads are documented here.

## 3.0.0 - 2026-09-26

- Move the filtering and decision policy into Brainfuck, compiled ahead of time to native code (`libtwitterbf.so`): the entry-identifier grammar and ambiguity rule, verdict precedence, Video Tab item classes and batch rules, promoted action names, render and Video boundary scoring and thresholds, the exact-profile version table and the discovery limits. Android, Xposed, DexKit, reflection and collection handling stay in Java. See `docs/BRAINFUCK_ARCHITECTURE.md` and the normative specs in `docs/policy/`.
- Adopt the ThreadsHideAds `v2.0.0` Brainfuck toolchain (source checker, IR optimizer, C emitter, reference interpreter, replay harness), with a per-request loop budget and a stack-only, stateless runtime.
- Keep the 2.1.0 Java policy as a frozen test oracle; the parity suite compares old and new results on 411 162 cases per run, plus randomized, malformed-frame, concurrency and invariant tests, on the JVM and on arm64.
- The render hot path makes one JNI call and allocates nothing: on a Pixel 8 Pro an organic post is classified in about 2.4 µs (was 14.9 µs), a promoted post in 1.1 µs (was 6.1 µs), a 20-item Video Tab batch in 6.8 µs (was 275 µs).
- Fail open on any policy failure: missing library or ABI mismatch installs no hook; oversized, malformed or budget-exhausted requests keep the content.
- Log per-render boundary observations only in debug builds, and drop the per-render diagnostic walk; add policy counters (calls, failures, fallback scans, blocks) to the block logs, with request timing in debug or `-PpolicyTiming` builds.
- CI: split into test, build and release jobs; check generated Brainfuck output, run the Python toolchain tests and the parity suite, verify every native library in the APK; signing secrets exist only in the release job.
- New launcher icon: a struck-through shield over the X mark, the strike running down the middle of the X's outlined bar, drawn as one vector that is also the themed icon (`docs/icon.svg`). The raster launcher and branding images are removed.
- Rewrite the README in the ThreadsHideAds layout.
- Increased Android `versionCode` from `33` to `34`.

Also in this release (previously unreleased):


- Classify timeline models into `PROMOTED`, `ORGANIC` or `UNKNOWN`. A confident organic entry identifier now short-circuits the reflective action-graph walk, which previously ran on every normal post rendered through an adaptive boundary, and the walk's result is cached per model instance.
- Match entry identifiers against a strict whole-value grammar. Look-alikes such as `not-promoted-x`, `unpromoted-1` or free text containing `promoted-` no longer count as promoted.
- Witness adaptively resolved boundaries at runtime and unhook any that see 12 invocations without a post-like model.
- Widen the `DexFile` fallback from `com.x.urt.items.post` to the `com.x.urt`, `com.x.jetfuel` and `com.x.mappers` namespaces, post package first.
- Build Video Tab replacements through the persistent list's own builder, resolved by shape because R8 strips it from the interface, and fail open otherwise. The dynamic-proxy list emulation is removed, and the caller stack is captured only for mixed batches that would be rewritten.
- Redact entry identifiers in release-build logs and bound the unique-block key set to 512 entries.
- Read the module version from `BuildConfig` and remove `assets/ad_patterns.json`, which was documentation only and had drifted from the resolver.
- Add JVM unit tests and run them in CI. Pin GitHub Actions to commit SHAs and verify the Gradle distribution checksum.
- Fix `scripts/check-project.sh`: negative checks written as `! cmd` never fail under `set -e`, so they are now explicit. The signing check asserts that no credential is hardcoded instead of forbidding the environment-backed `signingConfigs`.
- Validated on device against X `12.28.0-prod.01` under Vector 2.2.

## 2.1.0 - 2026-09-09

- Suppress promoted posts on every X surface instead of only the Home timeline. Post detail, search and any other surface that renders posts through the same Compose boundaries are now covered.
- Match the URT `promoted-` entry token at any `-` segment boundary rather than only at the start of the entry identifier. Only the Home timeline names a promoted entry `promoted-tweet-<id>-<hash>`; a surface that nests the post inside a module prefixes that module's own entry, so the same advertisement arrives as `conversationthread-<id>-promoted-tweet-<id>-<hash>` in a post detail and as `search-conversation-<id>-promoted-tweet-<id>-<hash>` in search. These compound identifiers failed the previous prefix test and rendered.
- Recognise a promoted-metadata field by any runtime class name ending in `PromotedMetadata` instead of only the validated `com.x.models.TimelinePromotedMetadata`, which X has obfuscated away since `12.22.0`.
- Accept nested entry identifiers when selecting which direct string field carries the entry ID, so post detail and search entries are reported in the logs instead of `unknown`.
- Raised the classifier schema version from `6` to `7`.
- Validated on device against X `12.23.1-prod.01` under Vector 2.2: 11 deoptimized boundaries installed, `enforcement=ACTIVE_ADAPTIVE`, promoted entries blocked before render on both the Home timeline and post detail, with normal posts and replies unaffected.
- Increased Android `versionCode` from `31` to `32`, then to `33` when the application id moved to `io.github.mrxsin.twitterhideads`.

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


