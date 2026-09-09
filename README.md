<div align="center">

# <img src="branding/twitter-hide-ads-icon.png" width="36" height="36"> Twitter Hide Ads

### A focused Xposed module for removing promoted posts from the X Android app

[![Android](https://img.shields.io/badge/Android-Vector-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/JingMatrix/Vector)
[![API](https://img.shields.io/badge/libxposed%20API-102-brightgreen?style=for-the-badge)](https://github.com/libxposed/api)
[![Target](https://img.shields.io/badge/Target-X-000000?style=for-the-badge&logo=x&logoColor=white)](https://x.com/)
[![DexKit](https://img.shields.io/badge/Powered%20by-DexKit-6A5ACD?style=for-the-badge)](https://github.com/LuckyPray/DexKit)
[![JDK](https://img.shields.io/badge/JDK-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)

</div>

<p align="center"><img src="branding/twitter-hide-ads-icon.png" width="160" alt="Twitter Hide Ads app icon"></p>

## ✨ Features

- Suppresses promoted posts wherever X renders them: Home timeline, post detail, search and every other surface sharing the post render boundaries.
- Filters promoted videos out of the full-screen Video Tab before pager creation.
- Waits for `Application.attach()` so X's final app class loader is ready before discovery.
- Uses DexKit to locate obfuscated render boundaries across app updates.
- Identifies boundaries by the post model they render, not by the package they are declared in.
- Deoptimizes resolved boundaries so ART cannot serve a compiled copy past the hook.
- Caches resolved boundaries by X version and APK fingerprint, and rescans after an X update.
- Fails open on unresolved boundaries so the timeline is never left blank.
- Limits its scope to the official X Android package.

## 🎯 Scope

The module hooks only:

```text
com.twitter.android
```

Do not enable additional applications in the module scope.

## 🔍 How it works

X ships a fully obfuscated, Compose-rendered timeline, and the names of the classes and methods involved change with almost every release. The module therefore resolves its hooks structurally at runtime:

1. The module entry class extends `io.github.libxposed.api.XposedModule` and installs a lightweight `Application.attach()` guard from `onPackageReady()` instead of running discovery immediately.
2. Once X supplies its real application context and final class loader, an exact compatibility profile is selected when the installed X version is one of the validated builds.
3. When no exact profile matches, adaptive structural resolution runs instead. DexKit enumerates the application's static `void` methods and the results are pre-filtered on the raw descriptor so that only methods taking a post model first are loaded through the host class loader.
4. Each surviving method is scored by role rather than by parameter position: a `Composer` followed by the compiler-generated `$changed` mask and optional `$default` mask, a post model in first position, and any `Modifier`, layout-scope or post-dependency parameters found in between.
5. Every boundary scoring at or above the activation threshold is hooked. The Kotlin Compose compiler emits both a defaulted and a non-defaulted entry point for one composable, so requiring a single unique winner used to resolve to no hook at all.
6. Each resolved boundary is deoptimized through `XposedInterface.deoptimize`. ART inlines these small composables into their callers, and without deoptimization the caller keeps running the compiled copy and the hook is never reached.
7. At render time the post model is classified from its direct fields: an entry identifier carrying the `promoted-` token, or a promoted-metadata field. A bounded action-graph walk is used as a fallback where the metadata class itself has been obfuscated away.
8. A promoted post is suppressed before Compose renders it. Normal posts are passed through untouched.

   Only the Home timeline names a promoted entry `promoted-tweet-<id>-<hash>`. Every surface that nests a post inside a module prefixes that module's own entry, so the same advertisement arrives as `conversationthread-<id>-promoted-tweet-<id>-<hash>` in a post detail and as `search-conversation-<id>-promoted-tweet-<id>-<hash>` in search. The token is therefore matched at any `-` segment boundary, which is what makes suppression app-wide rather than Home-only.
9. The Video Tab is handled separately at the data layer. The resolver identifies the URT state-copy method receiving the tab's mixed Kotlin immutable list and rebuilds a compatible immutable copy with promoted entries removed, preserving cursor and paging-control entries.

The runtime log for this revision identifies the active Home timeline boundary as:

```text
com.x.jetfuel.v2.element.attribute.h.h(com.x.urt.items.post.g5, Modifier, ..., Composer, int)
```

This boundary is declared outside the post package. Resolution keyed on the declaring package missed it entirely, which is why boundary identity is now derived from the post model instead.

## ✅ Requirements

### Runtime

- A framework implementing the modern Xposed API, version 101 or newer. The legacy `de.robv.android.xposed` API is no longer used, so frameworks that only implement it cannot load this module. Two supported options:
  - **Rooted:** [Vector](https://github.com/JingMatrix/Vector) on Android 8.1 or newer, with Magisk or KernelSU and Zygisk enabled.
  - **Rootless:** [LSPatch](https://github.com/JingMatrix/LSPatch), which embeds Vector into a patched X APK and loads modern libxposed modules through the same runtime. Use the `JingMatrix` fork; the archived `LSPosed/LSPatch` build predates the modern API and cannot load this module.
- The official X application (`com.twitter.android`).
- The **Twitter Hide Ads** APK installed and enabled in the framework manager, or baked into the patched APK in LSPatch integrated mode.

### Build environment

- Android Studio with JDK 17 or newer, **or** a standalone JDK 17+ setup.
- Gradle 9.6.1 when building without an existing wrapper.
- Android SDK Platform 36.
- Git or a downloaded copy of the project source.

## 🛠️ Build

From the project directory, build the release APK with the included wrapper:

#### Linux / macOS

```bash
./gradlew :app:assembleRelease
```

#### Windows PowerShell

```powershell
.\gradlew.bat :app:assembleRelease
```

The generated APK will be located under:

```text
app/build/outputs/apk/release/
```

Native libraries are packaged uncompressed. The framework loads a module's native code directly from inside the APK, and a compressed `libdexkit.so` cannot be loaded from that path.

## 📦 Installation

1. Build and install the release APK.
2. Open the framework manager. With LSPatch, patch X in manager mode and select the module there, or patch it in integrated mode with the module embedded.
3. Enable **Twitter Hide Ads**.
4. The module declares a static scope of `com.twitter.android` in `META-INF/xposed/scope.list`, so no scope selection is required.
5. Force-stop X once after installing or updating the module, then reopen it.
6. Review the framework logs for entries beginning with:

```text
[TwitterHideAds]
```

## 🧪 Validation status

The release version is `2.1.0` (`versionCode 32`). Suppression was validated on device against X `12.23.1-prod.01` under Vector 2.2: the adaptive resolver installed 11 deoptimized boundaries with `enforcement=ACTIVE_ADAPTIVE`, and promoted entries were blocked before render both on the Home timeline and inside post detail, where entry identifiers such as `conversationthread-<id>-promoted-tweet-<id>-<hash>` had previously been let through. Normal posts, replies and search results continued to render. Video Tab dataset filtering is unchanged from `1.2.0` and remains scoped to callers under `com.x.video.tab`.

The included GitHub Actions workflow builds on every push, pull request, and manual run. A `v*` tag additionally builds, signs, and attaches the release APK to the GitHub Release when the four signing secrets are configured.

## 🩺 Troubleshooting

| Problem | Suggested action |
| --- | --- |
| Promoted posts still appear | Confirm the module is enabled and check whether the log reports `enforcement=ACTIVE_ADAPTIVE`. `FAIL_OPEN_NO_BOUNDARY` means no boundary was resolved. |
| `couldn't find "libdexkit.so"` | The APK was built with compressed native libraries. Rebuild with `useLegacyPackaging = false`, which is the configured default. |
| Hooks install but nothing is blocked | Check `deoptimized=` in the initialization line. A value of `0` alongside `deoptimizeSupported=false` means the framework interface was never attached to the module entry. |
| `no-structural-candidate` after an update | X changed its render boundary shape. Capture the `DexKit structural query` and `Adaptive boundary` log lines for analysis. |
| Stale boundary after an X update | Resolution is cached by X version and APK fingerprint and should rescan automatically. Force-stop X once to trigger a fresh resolution. |

## 🙏 Credits

This project depends on and benefits from the following open-source work:

| Project | Contribution |
| --- | --- |
| [DexKit](https://github.com/LuckyPray/DexKit) by LuckyPray | High-performance runtime DEX parsing and discovery of obfuscated classes and methods. Licensed under Apache-2.0. |
| [Vector](https://github.com/JingMatrix/Vector) by JingMatrix | Provides the ART hooking framework, and the method deoptimization used to reach inlined composables. Licensed under GPL-3.0. |
| [LSPatch](https://github.com/JingMatrix/LSPatch) by JingMatrix | Embeds Vector into a patched APK, which is how this module runs without root. Licensed under GPL-3.0. |
| [libxposed API](https://github.com/libxposed/api) | The modern Xposed module API this module compiles against. Licensed under Apache-2.0. |

## ⚠️ Disclaimer

This project is not affiliated with, endorsed by, or sponsored by X Corp. or
Twitter. It is provided for educational and personal use. App updates may break
the hooks without notice.
