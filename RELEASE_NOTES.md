# Twitter Hide Ads v1.1.0

Twitter Hide Ads v1.1.0 restores promoted-post suppression after the X Android 12.8 update while retaining support for X 12.7.1.

## What changed

X 12.8.0 renamed the timeline render model and primary Compose boundary:

```text
X 12.7.1: a6$a -> c7.e(...)
X 12.8.0: w5$a -> d7.e(...)
```

This release adds an exact X 12.8.0 compatibility profile and selects the correct profile from the installed X version. The temporary diagnostic DEX scanner and broad observer hooks have been removed from the release build.

## Supported versions

- X `12.7.1` and suffix builds
- X `12.8.0` and suffix builds, including `12.8.0-release.0`

Unknown X versions fail open instead of applying an unverified hook.

## Detection and blocking

A post is suppressed before Compose rendering when:

- its timeline entry ID starts with `promoted-`; or
- its direct `TimelinePromotedMetadata` value is non-null.

Normal posts remain on the original X rendering path.

## Validation

The X 12.8.0 active build was runtime-tested and confirmed to block promoted posts successfully. The existing X 12.7.1 profile is retained.

## Upgrade

Install the v1.1.0 APK over the previous module, keep the LSPosed scope set to `com.twitter.android`, then force-stop and reopen X.
