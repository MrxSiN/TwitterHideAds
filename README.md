<div align="center">

# <img src="branding/twitter-hide-ads-icon.png" width="36" height="36"> Twitter Hide Ads

### A focused LSPosed module for removing promoted posts from the X Android app

[![Android](https://img.shields.io/badge/Android-LSPosed-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/LSPosed/LSPosed)
[![Target](https://img.shields.io/badge/Target-X-000000?style=for-the-badge&logo=x&logoColor=white)](https://x.com/)
[![DexKit](https://img.shields.io/badge/Powered%20by-DexKit-6A5ACD?style=for-the-badge)](https://github.com/LuckyPray/DexKit)
[![JDK](https://img.shields.io/badge/JDK-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)

</div>

<p align="center"><img src="branding/twitter-hide-ads-icon.png" width="160" alt="Twitter Hide Ads app icon"></p>

## ✨ Features

- Suppresses promoted posts in the X Home timeline before Compose renders them.
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

1. The LSPosed entry point installs a lightweight `Application.attach()` guard instead of running discovery immediately from `handleLoadPackage()`.
2. Once X supplies its real application context and final class loader, an exact compatibility profile is selected when the installed X version is one of the validated builds.
3. When no exact profile matches, adaptive structural resolution runs instead. DexKit enumerates the application's static `void` methods and the results are pre-filtered on the raw descriptor so that only methods taking a post model first are loaded through the host class loader.
4. Each surviving method is scored by role rather than by parameter position: a `Composer` followed by the compiler-generated `$changed` mask and optional `$default` mask, a post model in first position, and any `Modifier`, layout-scope or post-dependency parameters found in between.
5. Every boundary scoring at or above the activation threshold is hooked. The Kotlin Compose compiler emits both a defaulted and a non-defaulted entry point for one composable, so requiring a single unique winner used to resolve to no hook at all.
6. Each resolved boundary is deoptimized through LSPosed. ART inlines these small composables into their callers, and without deoptimization the caller keeps running the compiled copy and the hook is never reached.
7. At render time the post model is classified from its direct fields: a `promoted-` entry identifier, or a promoted-metadata field. A bounded action-graph walk is used as a fallback where the metadata class itself has been obfuscated away.
8. A promoted post is suppressed before Compose renders it. Normal posts are passed through untouched.
9. The Video Tab is handled separately at the data layer. The resolver identifies the URT state-copy method receiving the tab's mixed Kotlin immutable list and rebuilds a compatible immutable copy with promoted entries removed, preserving cursor and paging-control entries.

The runtime log for this revision identifies the active Home timeline boundary as:

```text
com.x.jetfuel.v2.element.attribute.h.h(com.x.urt.items.post.g5, Modifier, ..., Composer, int)
```

This boundary is declared outside the post package. Resolution keyed on the declaring package missed it entirely, which is why boundary identity is now derived from the post model instead.

## ✅ Requirements

### Runtime

- A rooted Android device.
- A working LSPosed installation.
- The official X application (`com.twitter.android`).
- The **Twitter Hide Ads** APK installed and enabled in LSPosed.

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

Native libraries are packaged uncompressed. LSPosed loads a module's native code directly from inside the APK, and a compressed `libdexkit.so` cannot be loaded from that path.

## 📦 Installation

1. Build and install the release APK.
2. Open the LSPosed manager.
3. Enable **Twitter Hide Ads**.
4. Select **X** as the module's only scope.
5. Force-stop X once after installing or updating the module, then reopen it.
6. Review LSPosed logs for entries beginning with:

```text
[TwitterHideAds]
```

## 🧪 Validation status

The release version is `1.3.0` (`versionCode 30`). Adaptive Home timeline suppression is confirmed active on X `12.22.0-prod.01`, resolving through the `com.x.jetfuel.v2.element.attribute` boundary and blocking promoted entries before render. Video Tab dataset filtering is unchanged from `1.2.0` and remains scoped to callers under `com.x.video.tab`.

The included GitHub Actions workflow builds on every push, pull request, and manual run. A `v*` tag additionally builds, signs, and attaches the release APK to the GitHub Release when the four signing secrets are configured.

## 🩺 Troubleshooting

| Problem | Suggested action |
| --- | --- |
| Promoted posts still appear | Confirm that only X is selected in the LSPosed scope, then check whether the log reports `enforcement=ACTIVE_ADAPTIVE`. `FAIL_OPEN_NO_BOUNDARY` means no boundary was resolved. |
| `couldn't find "libdexkit.so"` | The APK was built with compressed native libraries. Rebuild with `useLegacyPackaging = false`, which is the configured default. |
| Hooks install but nothing is blocked | Check `deoptimized=` in the initialization line. A value of `0` alongside `deoptimizeSupported=false` means the LSPosed build does not expose method deoptimization. |
| `no-structural-candidate` after an update | X changed its render boundary shape. Capture the `DexKit structural query` and `Adaptive boundary` log lines for analysis. |
| Stale boundary after an X update | Resolution is cached by X version and APK fingerprint and should rescan automatically. Force-stop X once to trigger a fresh resolution. |

## 🙏 Credits

This project depends on and benefits from the following open-source work:

| Project | Contribution |
| --- | --- |
| [DexKit](https://github.com/LuckyPray/DexKit) by LuckyPray | High-performance runtime DEX parsing and discovery of obfuscated classes and methods. Licensed under Apache-2.0. |
| [LSPosed](https://github.com/LSPosed/LSPosed) | Provides the Android runtime hooking framework, and the method deoptimization used to reach inlined composables. Licensed under GPL-3.0. |
| [Xposed API](https://github.com/rovo89/XposedBridge) by rovo89 | The hooking API this module compiles against. |

## ⚠️ Disclaimer

This project is not affiliated with, endorsed by, or sponsored by X Corp. or
Twitter. It is provided for educational and personal use. App updates may break
the hooks without notice.
