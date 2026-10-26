# Twitter Hide Ads v3.1.0

## Google SSP ads removed, and the module speaks 14 languages

X 12.37 started showing real-time-bidding ads served through Google SSP. They are not posts, so 3.0.0 never saw them: they appeared on Home, between conversation replies and as full pages in the Video Tab, while promoted posts were still blocked. This release removes them.

### Fixed

- Real-time-bidding ads (`UrtTimelineRtbImageAd`, the cards with an "Ad" label and an Open or Install button) are removed on Home, in conversations and in the Video Tab.
  - Video Tab and conversations: the ads are dropped from the timeline's paging state before it is built. A conversation module is dropped only when everything inside it is such an ad.
  - Home: an RTB ad is handed to X's own empty presenter, the one X itself uses when Google SSP is off, so nothing is drawn and no gap is left.
  - Event Summaries, which X 12.37.2 also renders through its Google SSP code, are left alone.
- The hooks are found by structure through DexKit once per X build and cached, so later starts add only a few milliseconds. Anything ambiguous installs nothing.
- The inlined-caller lookup no longer fails with `UnsatisfiedLinkError` when the boundary resolver was served from its cache.

### Added

- The module description shown in Vector and LSPosed is translated into Simplified and Traditional Chinese, Spanish, Portuguese, Russian, Japanese, Korean, Indonesian, Hindi, Arabic, Turkish, Vietnamese, French and German.
- A short user README in Simplified Chinese, Traditional Chinese, Spanish, Portuguese, Russian, Japanese, Korean and Indonesian, linked from a language bar on every README (#1).

### Verified

- On a Pixel 8 Pro, Android 17, Vector 2.2, X 12.37.2-prod.01: 3 full passes of 25 Home, 20 conversation and 20 Video Tab screens each showed no ad, with promoted posts still blocked and no crashes. Without the fix the same check found ads on most conversation screens and almost every Video Tab page.
- 74 JVM tests, including the 411 162-comparison policy parity suite and the new RTB filter tests.
- The first start after an X update spends about 3 s on DexKit discovery for the new hooks; later starts take 2–16 ms.

LSPatch, 32-bit and x86 devices were not run for this release.

### Version metadata

- Version name: `3.1.0`
- Android version code: `35`
- Git tag: `v3.1.0`
- APK filename: `TwitterHideAds-v3.1.0.apk`

### Updating

Install the v3.1.0 APK over v3.0.0, keep X (`com.twitter.android`) as the module's only scope, then force-stop and reopen X once. Confirm the framework log reports `enforcement=ACTIVE_ADAPTIVE` and `RTB ad filter initialized` with a `mapper=` other than `none`.
