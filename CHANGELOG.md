# Changelog

## 1.0.0 — 2026-07-16

- Published the first stable release of Twitter Hide Ads.
- Added active pre-render suppression for X Android 12.7.1.
- Added direct promoted-post classification using:
  - timeline entry IDs beginning with `promoted-`;
  - `com.x.models.TimelinePromotedMetadata` on the post render model.
- Added the confirmed promoted action enums as a bounded compatibility fallback:
  - `PromotedDismissAd`;
  - `PromotedAdsInfo`;
  - `PromotedReportAd`.
- Added the primary Compose boundary `com.x.urt.items.post.c7.e(...)`.
- Added conditional fallback boundaries that are installed only when the primary hook is unavailable.
- Added X-version detection at `Application.attach()` and compatibility profile `x-12.7.1`.
- Added fail-open behavior for unknown versions, missing boundaries, and hook errors.
- Removed the need for menu inspection, per-user learning, saved advertiser IDs, saved post IDs, or fixed feed-position rules.
- Reduced normal-operation overhead by disabling normal-post logs and avoiding duplicate fallback inspection.
- Added separate blocked-attempt and distinct-entry counters with rate-limited LSPosed output.
- Added GitHub Actions CI for checks, signed release builds, APK signature verification, workflow artifacts, and tagged GitHub Releases.
- Added release documentation, hook notes, release notes, and signing-secret instructions.

## Development history

The stable classifier was identified through internal diagnostic builds that traced X's post menu, promoted action enums, post options model, render model, and Compose call path. The public release series begins at `1.0.0`.
