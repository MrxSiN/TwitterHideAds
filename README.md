# Twitter Hide Ads

Twitter Hide Ads is an LSPosed module that removes promoted posts from the X Android timeline before the app's Compose renderer draws them.

## Release

- Module version: `1.1.0`
- Android version code: `22`
- Module package: `my.MrxSiN.twitterhideads`
- LSPosed scope: `com.twitter.android`
- Supported X versions:
  - `12.7.1` and suffix builds such as `12.7.1-release.0`
  - `12.8.0` and suffix builds such as `12.8.0-release.0`

## Features

- Removes promoted timeline posts before Compose rendering.
- Uses direct promoted-post signals rather than advertiser lists or post position.
- Supports separate hook profiles for X 12.7.1 and X 12.8.0.
- Keeps normal posts on the original rendering path.
- Fails open when the installed X version or hook boundary is unsupported.
- Uses no network access, database, DEX scan, global View hooks, or deoptimization.

## How it works

The module selects a compatibility profile from the installed X version and hooks an exact `void` Compose post-render method. Before the original method executes, the first post model is classified as promoted when either:

- its entry ID begins with `promoted-`; or
- its direct `TimelinePromotedMetadata` field is non-null.

A promoted post is suppressed with `param.setResult(null)`. Normal posts continue into the original X renderer.

### X 12.7.1 profile

- Render model: `com.x.urt.items.post.a6$a`
- Primary boundary: `com.x.urt.items.post.c7.e(...)`
- Fallback boundaries:
  - `com.x.urt.items.post.e.a(...)`
  - `com.x.urt.items.post.c7.a(...)`

### X 12.8.0 profile

- Render model: `com.x.urt.items.post.w5$a`
- Primary boundary: `com.x.urt.items.post.d7.e(...)`
- Fallback boundaries:
  - `com.x.urt.items.post.e.a(...)`
  - `com.x.urt.items.post.d7.a(...)`

Fallbacks are installed only when the corresponding primary boundary cannot be installed.

## Installation

1. Install the release APK.
2. Enable **Twitter Hide Ads** in LSPosed.
3. Select only the X app (`com.twitter.android`) as its scope.
4. Force-stop X and open it again.

Expected startup log on X 12.8.0:

```text
Twitter Hide Ads v1.1.0: loading in com.twitter.android
[TwitterHideAds] Detected X version=12.8.0-release.0, versionCode=..., selectedProfile=x-12.8.0
[TwitterHideAds] Installed pre-render boundary [primary-d7.e]: ...
[TwitterHideAds] Initialization complete: primaryHooks=1, ... enforcement=ACTIVE_PRIMARY
```

Expected block log:

```text
[TwitterHideAds] Blocked promoted post before Compose: boundary=primary-d7.e, entryId=promoted-tweet-..., signals=[entryId:promoted-, TimelinePromotedMetadata]
```

## Compatibility behavior

The hook mappings use obfuscated X classes and may change after an X update. Unsupported versions do not receive a guessed hook. The module logs `FAIL_OPEN_NO_BOUNDARY` or an unsupported-version message and leaves the X timeline unchanged.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36
- Gradle wrapper included in the repository

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Generated APK names include the module version:

```text
TwitterHideAds-v1.1.0.apk
```

## GitHub Actions release

The included workflow follows the release process used by ThreadsHideAds:

1. Build the unsigned release APK with Gradle.
2. Sign it using `r0adkll/sign-android-release@v1`.
3. Attach the signed APK to the tagged GitHub release.

Required repository secrets:

- `SIGNING_KEY`
- `ALIAS`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- `TOKEN`

Create and push a tag matching the app version:

```bash
git tag v1.1.0
git push origin v1.1.0
```

## Privacy and scope

- No network permission or outbound requests.
- No collection of account, post, advertiser, or analytics data.
- No persistent storage.
- No modification outside `com.twitter.android`.

## License

See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).
