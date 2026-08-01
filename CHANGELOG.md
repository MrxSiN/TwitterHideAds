## 1.2.6-test-app-icon

- Added a custom Twitter Hide Ads launcher icon.
- Added legacy launcher assets for mdpi through xxxhdpi.
- Added adaptive, round, and monochrome themed icon resources.
- No blocker logic changes from 1.2.5-test-video-dataset-filter.

# Changelog

## 1.2.5-test-video-dataset-filter

- Replaced the read-only Video Tab dataset diagnostic with one active upstream filter.
- Dynamically resolves the direct URT state-copy method whose first parameter and return type match and whose second parameter is a Kotlin immutable list.
- Requires a `com.x.video.tab.*` runtime caller before filtering.
- Removes only direct promoted timeline-post entries from mixed normal/promoted video batches.
- Preserves normal videos, cursors, headers and paging-control objects.
- Reconstructs a compatible immutable list and verifies the result before replacing the method argument.
- Caches the resolved method by X version and APK fingerprint.
- Fails open on ambiguous resolution, incompatible immutable-list copying or failed verification.
- Installs no video Compose, autoplay, playback, ExoPlayer, player-stop or dynamic caller hooks.

## 1.2.4-test-video-dataset-diagnostic

- Removed the broad experimental video hook stack.
- Added a read-only data-layer diagnostic that identified `com.x.urt.o0$d.a(...)` as the mixed Video Tab batch boundary on X 12.10.1.

## 1.2.0-test-adaptive

- Added adaptive structural resolution for the Home timeline pre-render boundary.
