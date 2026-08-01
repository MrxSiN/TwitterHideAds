# Twitter Hide Ads

An LSPosed module that suppresses promoted posts in the X Android Home timeline and filters promoted videos from the full-screen Video Tab before pager creation.

## Active dataset-filter test build

```text
versionName: 1.2.6-test-app-icon
versionCode: 29
```

The existing adaptive Home timeline blocker is unchanged. The video implementation has been rebuilt around one upstream data hook rather than renderer, autoplay or player hooks.

## Current behavior

```text
Home timeline ads: ACTIVE adaptive blocking
Video Tab ads:    ACTIVE upstream dataset filtering
```

## Video strategy

The resolver dynamically identifies the URT state-copy method that receives the Video Tab's mixed Kotlin immutable list. The runtime hook activates only when:

- the caller stack contains `com.x.video.tab.*`;
- the list contains multiple direct timeline-post items;
- at least one normal `tweet-*` item is present;
- at least one `promoted-*` item or non-null `TimelinePromotedMetadata` is present.

A compatible immutable copy is created with promoted posts removed. Cursor and paging-control entries are preserved.

```text
Video feed batch
→ remove promoted UrtTimelinePost entries
→ pager receives normal videos and paging entries
→ promoted playback is never created
```

## Hook footprint

```text
Video dataset hooks: 1
Compose hooks:       0
Autoplay hooks:      0
Playback hooks:      0
Player hooks:        0
Dynamic caller hooks: 0
```

The resolved method descriptor is cached using the X version and APK fingerprint. A renamed method is rescanned after an X update.

## Expected log

```text
Video dataset resolver selected: ...
Video dataset filter initialized: ... installedHooks=1
Filtered promoted videos before pager creation: originalSize=13, filteredSize=11, removed=2
```

If a compatible immutable copy cannot be created or verified, the video filter fails open and leaves the original batch unchanged.

## Test procedure

1. Install the APK over the previous test build.
2. Enable it for `com.twitter.android` in LSPosed.
3. Force-stop X and reopen it.
4. Confirm Home timeline advertisements remain blocked.
5. Open the full-screen Video Tab.
6. Scroll through at least 20 videos.
7. Confirm advertisements are skipped completely, with no blank page or continuing audio.
8. Check that normal swiping and playback remain smooth.
9. Export the LSPosed log.

## Disclaimer

This project is independent and is not affiliated with X Corp., Twitter, LSPosed or DexKit. Internal X structures can change without notice.
