# Changelog

All notable changes to Twitter Hide Ads are documented here.

## 1.1.0 - 2026-07-17

### Added

- Stable promoted-post suppression for X `12.8.0-release.0`.
- A dedicated X 12.8.0 compatibility profile using:
  - `com.x.urt.items.post.w5$a`
  - `com.x.urt.items.post.d7.e(...)`
  - `com.x.urt.items.post.e.a(...)`
  - `com.x.urt.items.post.d7.a(...)`
- Version-aware selection between the validated X 12.7.1 and X 12.8.0 hook profiles.

### Changed

- Updated promoted-post model detection for the X 12.8.0 `w5$a` model.
- Replaced the temporary X 12.8.0 diagnostic scanner with exact, lightweight render hooks.
- Kept fallback hooks inactive unless the primary render boundary is unavailable.
- Updated release documentation and project metadata for the stable GitHub release.
- Increased Android `versionCode` from `18` to `22` across the development and release cycle.

## 1.0.0

- First stable release for X 12.7.1.
- Added promoted-post suppression before Compose rendering.
