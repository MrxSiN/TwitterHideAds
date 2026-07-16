# Twitter Hide Ads

### A focused LSPosed module that removes promoted posts from the X Android timeline before they are rendered

[![Android](https://img.shields.io/badge/Android-LSPosed-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/LSPosed/LSPosed)
[![Target](https://img.shields.io/badge/Target-X%20Android-000000?style=for-the-badge&logo=x&logoColor=white)](https://play.google.com/store/apps/details?id=com.twitter.android)
[![Release](https://img.shields.io/badge/Release-v1.0.0-2ea44f?style=for-the-badge)](https://github.com/MrxSiN/TwitterHideAds/releases)
[![JDK](https://img.shields.io/badge/JDK-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)

## Features

- Removes promoted timeline posts before the Compose renderer draws them.
- Uses X-owned promoted metadata rather than text matching, advertiser lists, or fixed feed positions.
- Works immediately after installation; no per-user learning stage is required.
- Installs one primary hook during normal operation and uses fallbacks only when the primary boundary is unavailable.
- Fails open on unsupported X versions instead of applying an untested obfuscated field mapping.
- Does not use DexKit, global view hooks, accessibility scanning, network access, or an advertiser database.
- Uses rate-limited LSPosed logging suitable for normal use.

## Scope

The module is intended to be enabled only for:

```text
com.twitter.android
```

Do not select additional applications in the LSPosed module scope.

## Compatibility

| Component | Supported release |
| --- | --- |
| X Android package | `com.twitter.android` |
| Validated X version | `12.7.1` |
| Android | API 24 or newer |
| LSPosed API | 93 or newer |
| Module release | `1.0.0` |

The installed X version is checked during `Application.attach()`. Version profile `x-12.7.1` is activated only for compatible `12.7.1` version strings. Unknown versions enter fail-open mode and receive no rendering hooks.

## How it works

The validated X build converts each timeline entry into a post render model before invoking its Compose renderer. Promoted posts expose two direct signals:

```text
entryId begins with "promoted-"
```

and:

```text
com.x.models.TimelinePromotedMetadata is present on the render model
```

The module hooks this primary pre-render boundary:

```text
com.x.urt.items.post.c7.e(...): void
```

Its first argument implements `com.x.urt.items.post.a6` and is normally the concrete render model `com.x.urt.items.post.a6$a`. When the model is classified as promoted, the before-hook calls `param.setResult(null)`, so the Compose method returns without drawing the post.

Fallback boundaries are installed only when the primary method is unavailable:

```text
com.x.urt.items.post.e.a(...): void
com.x.urt.items.post.c7.a(...): void
```

The confirmed ad-only post actions remain bundled as a secondary classifier for unfamiliar model shapes:

```text
PromotedDismissAd
PromotedAdsInfo
PromotedReportAd
```

Detailed implementation notes are available in [HOOK_NOTES.md](HOOK_NOTES.md).

## Safety design

- **Version guarded:** obfuscated mappings are enabled only for the validated X profile.
- **Fail open:** missing classes, missing methods, or unsupported versions leave the original X behavior intact.
- **Primary hook first:** normal posts are not inspected repeatedly through all fallback renderers.
- **Direct model signals:** normal text containing words such as “ad” or “promoted” is not used for classification.
- **No position rule:** the module does not assume that an advertisement is always the second timeline item.
- **No persistent tracking:** advertiser IDs and post IDs are not saved.
- **No network access:** all classification runs locally in the X process.

## Requirements

### Runtime

- A rooted Android device.
- A working LSPosed installation.
- The official X Android application.
- The Twitter Hide Ads APK installed and enabled in LSPosed.

### Build environment

- Android Studio or JDK 17+.
- Android SDK Platform 37.
- The included Gradle 9.5 wrapper.
- Git, or a downloaded copy of this source tree.

## Build

From the project root:

```bash
./gradlew clean assembleRelease
```

On Windows:

```powershell
.\gradlew.bat clean assembleRelease
```

Unsigned local release output is written under:

```text
app/build/outputs/apk/release/
```

The GitHub Actions workflow normalizes the artifact name to:

```text
TwitterHideAds-v1.0.0.apk
```

## Installation

1. Install the release APK.
2. Open LSPosed Manager.
3. Enable **Twitter Hide Ads**.
4. Select only **X** (`com.twitter.android`) as the module scope.
5. Force-stop X and reopen it.
6. Review LSPosed logs for entries beginning with `[TwitterHideAds]` if the hook does not activate.

Expected startup output:

```text
Twitter Hide Ads v1.0.0: loading in com.twitter.android
[TwitterHideAds] Application attach guard installed
[TwitterHideAds] Detected X version=12.7.1..., selectedProfile=x-12.7.1
[TwitterHideAds] Installed pre-render boundary [primary-c7.e]: ...
[TwitterHideAds] Initialization complete: ... enforcement=ACTIVE_PRIMARY
```

## Validation status

Version 1.0.0 is based on on-device testing against X 12.7.1. Runtime logs confirmed that the primary pre-render hook blocked multiple distinct `promoted-tweet-*` entries carrying `TimelinePromotedMetadata`, while normal `tweet-*` entries continued through the renderer. No advertisement was observed during the final refresh and scrolling test.

App updates may change obfuscated classes, fields, or method signatures. An unsupported X version will be reported in LSPosed logs and will fail open.

## Troubleshooting

| Problem | Suggested action |
| --- | --- |
| Promoted posts still appear | Confirm X 12.7.1 is installed, confirm only `com.twitter.android` is selected, then force-stop and reopen X. |
| No module log entries | Confirm the module is enabled, verify LSPosed is active, then reboot the device. |
| Unsupported-version message | The installed X version does not match the bundled compatibility profile. Do not force the hook; provide the startup log for analysis. |
| No render boundary found | X changed the obfuscated rendering path. Export the `[TwitterHideAds]` initialization lines. |
| Blank timeline space | Export the block and initialization logs and note which timeline was open. |
| X crashes after an update | Disable the module for X, reopen X, and provide the LSPosed exception log. |

## Repository files

- `README.md` — usage, build, installation, and release instructions.
- `CHANGELOG.md` — release history.
- `HOOK_NOTES.md` — validated hook path and classifier details.
- `RELEASE_NOTES.md` — text used for the GitHub v1.0.0 release.
- `.github/workflows/android.yml` — CI, signing, artifact upload, and GitHub Release publishing.
- `app/src/main/assets/ad_patterns.json` — human-readable compatibility profile.

## Credits

- [LSPosed](https://github.com/LSPosed/LSPosed) provides the runtime hooking framework.
- X/Twitter owns the target application, data models, and trademarks referenced by this compatibility module.

## Disclaimer

This project is not affiliated with, endorsed by, or sponsored by X Corp., Twitter, or LSPosed. It is provided for educational and personal use. Application updates may break compatibility without notice.
