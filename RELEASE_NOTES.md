# Twitter Hide Ads v1.0.0

First stable release of the LSPosed module for removing promoted posts from X Android.

## Highlights

- Blocks promoted posts before the Compose renderer draws them.
- Validated against X Android 12.7.1 (`com.twitter.android`).
- Uses X-owned `promoted-` timeline entry IDs and `TimelinePromotedMetadata`.
- Requires no menu opening, learning database, advertiser list, or saved post IDs.
- Installs only the primary render hook during normal operation.
- Fails open on unsupported X versions.
- Uses bounded fallback detection and rate-limited LSPosed logs.

## Installation

1. Install `TwitterHideAds-v1.0.0.apk`.
2. Enable the module in LSPosed.
3. Select only X (`com.twitter.android`) as the module scope.
4. Force-stop X and reopen it.

## Compatibility

This release is validated for X 12.7.1. Later X versions may change internal obfuscated classes or fields. Unsupported versions are intentionally left unmodified and are reported in LSPosed logs.
